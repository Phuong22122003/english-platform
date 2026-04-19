package com.english.user_service.annotation;

import com.english.user_service.enums.StreamAction;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SyncUserStream {
    StreamAction action();
}
