package handlers

import arrow.core.Option
import com.google.devtools.ksp.symbol.KSValueParameter
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ksp.toTypeName

fun List<KSValueParameter>.toPoetParameterSpecs(): List<ParameterSpec> = this.map {
    val name = it.name!!.asString()
    val type = it.type.resolve().toTypeName()
    ParameterSpec.builder(name, type).build()
}

fun List<KSValueParameter>.findArrowOptionParamNames(): List<String> = this.filter {
    it.type.resolve().declaration.qualifiedName?.asString() == Option::class.qualifiedName!!
}.map { it.name!!.asString() }
