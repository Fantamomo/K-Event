package com.fantamomo.kevent.processor

import com.google.devtools.ksp.isLocal
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*

class EventHandlerProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val annotatedMethods = resolver.getSymbolsWithAnnotation("com.fantamomo.kevent.Register")
            .filterIsInstance<KSFunctionDeclaration>().filter { !it.isLocal() }

        val byClass = annotatedMethods.groupBy { it.parentDeclaration as KSClassDeclaration }

        byClass.forEach { (listenerClass, methods) ->
            generateRegistry(listenerClass, methods)
        }

        return emptyList()
    }

    private fun generateRegistry(listener: KSClassDeclaration, methods: List<KSFunctionDeclaration>) {
        val pkg = listener.packageName.asString()
        val className = listener.simpleName.asString()
        val registryName = "${className}HandlerRegistry"

        val file = codeGenerator.createNewFile(
            Dependencies(false, *listener.containingFile?.let { arrayOf(it) } ?: emptyArray()),
            pkg,
            registryName
        )

        file.bufferedWriter().use { out ->
            out.appendLine("package $pkg")
            out.appendLine()
            out.appendLine("import com.fantamomo.kevent.*")
            out.appendLine("import com.fantamomo.kevent.processor.*")
            out.appendLine("import kotlin.reflect.KClass")
            out.appendLine("import kotlin.reflect.KFunction")
            out.appendLine()
            out.appendLine("@OptIn(InternalProcessorApi::class)")
            out.appendLine("@GeneratedHandlerRegistry($className::class)")
            out.appendLine("object $registryName : EventHandlerRegistry {")
            out.appendLine("    override val listeners: Array<HandlerDefinition> = arrayOf(")

            methods.forEachIndexed { idx, fn ->
                val params = fn.parameters
                val eventParam = params.firstOrNull()
                val eventType = eventParam?.type?.resolve()?.declaration?.qualifiedName?.asString() ?: "com.fantamomo.kevent.api.Dispatchable"
                val nullable = eventParam?.type?.resolve()?.nullability == Nullability.NULLABLE
                val isSuspend = fn.modifiers.contains(Modifier.SUSPEND)

                val argsArray = if (params.size > 1) {
                    params.drop(1).joinToString(", ") { p ->
                        val pname = (p.annotations.firstOrNull {
                            it.annotationType.resolve().declaration.qualifiedName?.asString() == "com.fantamomo.kevent.utils.InjectionName"
                        }?.arguments?.firstOrNull { it.name?.asString() == "value" }?.value as? String) ?: p.name?.asString() ?: throw IllegalArgumentException("Parameter ${p.name?.asString()} has no name!")
                        val ptype = p.type.resolve().declaration.qualifiedName?.asString() ?: "kotlin.Any"
                        "ParameterDefinition(\"$pname\", $ptype::class)"
                    }
                } else ""

                out.appendLine("        HandlerDefinition(")
                out.appendLine("            listener = $className::class,")
                out.appendLine("            method = $className::${fn.simpleName.asString()},")
                out.appendLine("            event = $eventType::class,")
                out.appendLine("            args = arrayOf($argsArray),")
                out.appendLine("            isSuspend = $isSuspend,")
                out.appendLine("            isNullable = $nullable")
                out.appendLine("        ),")
            }

            out.appendLine("    )")
            out.appendLine("}")
        }

        val serviceFile = codeGenerator.createNewFileByPath(
            Dependencies(false),
            "META-INF.services/com.fantamomo.kevent.api.EventHandlerRegistry",
            ""
        )
        serviceFile.bufferedWriter().use { out ->
            out.appendLine("$pkg.$registryName")
        }
    }
}
