package com.gymiq.aop;

import com.gymiq.enums.AuditAction;
import com.gymiq.enums.ResourceType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {

    AuditAction action();

    ResourceType resourceType();

    String description();
}
