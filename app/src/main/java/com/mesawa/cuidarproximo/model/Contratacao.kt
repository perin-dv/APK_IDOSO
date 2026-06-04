package com.mesawa.cuidarproximo.model

data class Contratacao(

    var id: String = "",

    var cuidadorId: String = "",

    var cuidadorNome: String = "",

    var familiarId: String = "",

    var idosoNome: String = "",

    var endereco: String = "",

    var observacao: String = "",

    var horas: Int = 0,

    var valor: Double = 0.0,

    var status: String = "aguardando_pagamento",

    var timestamp: Long = System.currentTimeMillis()
)