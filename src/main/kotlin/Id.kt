package com.epam.kodux

import kotlinx.serialization.*

@SerialInfo
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS)
annotation class Id