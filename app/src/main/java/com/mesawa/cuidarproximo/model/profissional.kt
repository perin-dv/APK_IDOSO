package com.mesawa.cuidarproximo.model

data class Profissional(
    val nome: String = "",
    val especialidade: String = "",
    val avaliacao: Double = 0.0,
    val atendimentos: Int = 0,
    val valorHora: Double = 0.0,
    val ativo: Boolean = true
)