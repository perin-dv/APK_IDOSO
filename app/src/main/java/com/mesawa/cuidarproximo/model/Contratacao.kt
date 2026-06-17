package com.mesawa.cuidarproximo.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class Contratacao(

    @DocumentId
    var id: String = "",

    var clienteId: String = "",
    var cuidadorId: String = "",
    var cuidadorNome: String = "",

    var idosoNome: String = "",
    var endereco: String = "",
    var observacao: String = "",

    var horas: Int = 0,

    var valorTotal: Double = 0.0,
    var taxaPlataforma: Double = 0.09,
    var valorComissao: Double = 0.0,
    var valorLiquidoCuidador: Double = 0.0,

    var status: String = "aguardando_pagamento",
    var pagamentoStatus: String = "pending",
    var metodoPagamento: String = "pix",
    var paymentId: String? = null,

    @ServerTimestamp
    var createdAt: Timestamp? = null,

    @ServerTimestamp
    var updatedAt: Timestamp? = null
)