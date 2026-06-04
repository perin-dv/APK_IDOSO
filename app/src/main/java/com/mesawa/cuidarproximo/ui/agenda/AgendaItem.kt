package com.mesawa.cuidarproximo.ui.agenda


data class AgendaItem(

    val hora: String = "",

    val titulo: String = "",

    val descricao: String = "",

    val categoria: String = "",

    val prioridade: String = "",

    val repeticao: String = "",

    val concluido: Boolean = false,

    val timestamp: Long = 0,

    var id: String = "",


    val atrasado: Boolean = false,
)