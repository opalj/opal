; ModuleID = 'TaintTest.c'
source_filename = "TaintTest.c"
target datalayout = "e-m:e-p270:32:32-p271:32:32-p272:64:64-i64:64-f80:128-n8:16:32:64-S128"
target triple = "x86_64-pc-linux-gnu"

%struct.JNINativeInterface_ = type { i8*, i8*, i8*, i8*, i32 (%struct.JNINativeInterface_**)*, %struct._jobject* (%struct.JNINativeInterface_**, i8*, %struct._jobject*, i8*, i32)*, %struct._jobject* (%struct.JNINativeInterface_**, i8*)*, %struct._jmethodID* (%struct.JNINativeInterface_**, %struct._jobject*)*, %struct._jfieldID* (%struct.JNINativeInterface_**, %struct._jobject*)*, %struct._jobject* (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, i8)*, %struct._jobject* (%struct.JNINativeInterface_**, %struct._jobject*)*, i8 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jobject*)*, %struct._jobject* (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*, i8)*, i32 (%struct.JNINativeInterface_**, %struct._jobject*)*, i32 (%struct.JNINativeInterface_**, %struct._jobject*, i8*)*, %struct._jobject* (%struct.JNINativeInterface_**)*, void (%struct.JNINativeInterface_**)*, void (%struct.JNINativeInterface_**)*, void (%struct.JNINativeInterface_**, i8*)*, i32 (%struct.JNINativeInterface_**, i32)*, %struct._jobject* (%struct.JNINativeInterface_**, %struct._jobject*)*, %struct._jobject* (%struct.JNINativeInterface_**, %struct._jobject*)*, void (%struct.JNINativeInterface_**, %struct._jobject*)*, void (%struct.JNINativeInterface_**, %struct._jobject*)*, i8 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jobject*)*, %struct._jobject* (%struct.JNINativeInterface_**, %struct._jobject*)*, i32 (%struct.JNINativeInterface_**, i32)*, %struct._jobject* (%struct.JNINativeInterface_**, %struct._jobject*)*, %struct._jobject* (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, ...)*, %struct._jobject* (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %struct.__va_list_tag*)*, %struct._jobject* (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %union.jvalue*)*, %struct._jobject* (%struct.JNINativeInterface_**, %struct._jobject*)*, i8 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jobject*)*, %struct._jmethodID* (%struct.JNINativeInterface_**, %struct._jobject*, i8*, i8*)*, %struct._jobject* (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, ...)*, %struct._jobject* (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %struct.__va_list_tag*)*, %struct._jobject* (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %union.jvalue*)*, i8 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, ...)*, i8 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %struct.__va_list_tag*)*, i8 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %union.jvalue*)*, i8 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, ...)*, i8 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %struct.__va_list_tag*)*, i8 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %union.jvalue*)*, i16 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, ...)*, i16 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %struct.__va_list_tag*)*, i16 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %union.jvalue*)*, i16 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, ...)*, i16 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %struct.__va_list_tag*)*, i16 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %union.jvalue*)*, i32 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, ...)*, i32 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %struct.__va_list_tag*)*, i32 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %union.jvalue*)*, i64 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, ...)*, i64 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %struct.__va_list_tag*)*, i64 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %union.jvalue*)*, float (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, ...)*, float (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %struct.__va_list_tag*)*, float (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %union.jvalue*)*, double (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, ...)*, double (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %struct.__va_list_tag*)*, double (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %union.jvalue*)*, void (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, ...)*, void (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %struct.__va_list_tag*)*, void (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %union.jvalue*)*, %struct._jobject* (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jobject*, %struct._jmethodID*, ...)*, %struct._jobject* (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jobject*, %struct._jmethodID*, %struct.__va_list_tag*)*, %struct._jobject* (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jobject*, %struct._jmethodID*, %union.jvalue*)*, i8 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jobject*, %struct._jmethodID*, ...)*, i8 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jobject*, %struct._jmethodID*, %struct.__va_list_tag*)*, i8 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jobject*, %struct._jmethodID*, %union.jvalue*)*, i8 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jobject*, %struct._jmethodID*, ...)*, i8 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jobject*, %struct._jmethodID*, %struct.__va_list_tag*)*, i8 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jobject*, %struct._jmethodID*, %union.jvalue*)*, i16 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jobject*, %struct._jmethodID*, ...)*, i16 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jobject*, %struct._jmethodID*, %struct.__va_list_tag*)*, i16 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jobject*, %struct._jmethodID*, %union.jvalue*)*, i16 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jobject*, %struct._jmethodID*, ...)*, i16 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jobject*, %struct._jmethodID*, %struct.__va_list_tag*)*, i16 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jobject*, %struct._jmethodID*, %union.jvalue*)*, i32 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jobject*, %struct._jmethodID*, ...)*, i32 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jobject*, %struct._jmethodID*, %struct.__va_list_tag*)*, i32 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jobject*, %struct._jmethodID*, %union.jvalue*)*, i64 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jobject*, %struct._jmethodID*, ...)*, i64 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jobject*, %struct._jmethodID*, %struct.__va_list_tag*)*, i64 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jobject*, %struct._jmethodID*, %union.jvalue*)*, float (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jobject*, %struct._jmethodID*, ...)*, float (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jobject*, %struct._jmethodID*, %struct.__va_list_tag*)*, float (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jobject*, %struct._jmethodID*, %union.jvalue*)*, double (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jobject*, %struct._jmethodID*, ...)*, double (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jobject*, %struct._jmethodID*, %struct.__va_list_tag*)*, double (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jobject*, %struct._jmethodID*, %union.jvalue*)*, void (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jobject*, %struct._jmethodID*, ...)*, void (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jobject*, %struct._jmethodID*, %struct.__va_list_tag*)*, void (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jobject*, %struct._jmethodID*, %union.jvalue*)*, %struct._jfieldID* (%struct.JNINativeInterface_**, %struct._jobject*, i8*, i8*)*, %struct._jobject* (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*)*, i8 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*)*, i8 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*)*, i16 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*)*, i16 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*)*, i32 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*)*, i64 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*)*, float (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*)*, double (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*)*, void (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*, %struct._jobject*)*, void (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*, i8)*, void (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*, i8)*, void (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*, i16)*, void (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*, i16)*, void (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*, i32)*, void (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*, i64)*, void (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*, float)*, void (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*, double)*, %struct._jmethodID* (%struct.JNINativeInterface_**, %struct._jobject*, i8*, i8*)*, %struct._jobject* (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, ...)*, %struct._jobject* (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %struct.__va_list_tag*)*, %struct._jobject* (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %union.jvalue*)*, i8 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, ...)*, i8 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %struct.__va_list_tag*)*, i8 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %union.jvalue*)*, i8 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, ...)*, i8 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %struct.__va_list_tag*)*, i8 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %union.jvalue*)*, i16 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, ...)*, i16 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %struct.__va_list_tag*)*, i16 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %union.jvalue*)*, i16 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, ...)*, i16 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %struct.__va_list_tag*)*, i16 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %union.jvalue*)*, i32 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, ...)*, i32 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %struct.__va_list_tag*)*, i32 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %union.jvalue*)*, i64 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, ...)*, i64 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %struct.__va_list_tag*)*, i64 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %union.jvalue*)*, float (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, ...)*, float (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %struct.__va_list_tag*)*, float (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %union.jvalue*)*, double (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, ...)*, double (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %struct.__va_list_tag*)*, double (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %union.jvalue*)*, void (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, ...)*, void (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %struct.__va_list_tag*)*, void (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %union.jvalue*)*, %struct._jfieldID* (%struct.JNINativeInterface_**, %struct._jobject*, i8*, i8*)*, %struct._jobject* (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*)*, i8 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*)*, i8 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*)*, i16 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*)*, i16 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*)*, i32 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*)*, i64 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*)*, float (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*)*, double (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*)*, void (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*, %struct._jobject*)*, void (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*, i8)*, void (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*, i8)*, void (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*, i16)*, void (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*, i16)*, void (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*, i32)*, void (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*, i64)*, void (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*, float)*, void (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*, double)*, %struct._jobject* (%struct.JNINativeInterface_**, i16*, i32)*, i32 (%struct.JNINativeInterface_**, %struct._jobject*)*, i16* (%struct.JNINativeInterface_**, %struct._jobject*, i8*)*, void (%struct.JNINativeInterface_**, %struct._jobject*, i16*)*, %struct._jobject* (%struct.JNINativeInterface_**, i8*)*, i32 (%struct.JNINativeInterface_**, %struct._jobject*)*, i8* (%struct.JNINativeInterface_**, %struct._jobject*, i8*)*, void (%struct.JNINativeInterface_**, %struct._jobject*, i8*)*, i32 (%struct.JNINativeInterface_**, %struct._jobject*)*, %struct._jobject* (%struct.JNINativeInterface_**, i32, %struct._jobject*, %struct._jobject*)*, %struct._jobject* (%struct.JNINativeInterface_**, %struct._jobject*, i32)*, void (%struct.JNINativeInterface_**, %struct._jobject*, i32, %struct._jobject*)*, %struct._jobject* (%struct.JNINativeInterface_**, i32)*, %struct._jobject* (%struct.JNINativeInterface_**, i32)*, %struct._jobject* (%struct.JNINativeInterface_**, i32)*, %struct._jobject* (%struct.JNINativeInterface_**, i32)*, %struct._jobject* (%struct.JNINativeInterface_**, i32)*, %struct._jobject* (%struct.JNINativeInterface_**, i32)*, %struct._jobject* (%struct.JNINativeInterface_**, i32)*, %struct._jobject* (%struct.JNINativeInterface_**, i32)*, i8* (%struct.JNINativeInterface_**, %struct._jobject*, i8*)*, i8* (%struct.JNINativeInterface_**, %struct._jobject*, i8*)*, i16* (%struct.JNINativeInterface_**, %struct._jobject*, i8*)*, i16* (%struct.JNINativeInterface_**, %struct._jobject*, i8*)*, i32* (%struct.JNINativeInterface_**, %struct._jobject*, i8*)*, i64* (%struct.JNINativeInterface_**, %struct._jobject*, i8*)*, float* (%struct.JNINativeInterface_**, %struct._jobject*, i8*)*, double* (%struct.JNINativeInterface_**, %struct._jobject*, i8*)*, void (%struct.JNINativeInterface_**, %struct._jobject*, i8*, i32)*, void (%struct.JNINativeInterface_**, %struct._jobject*, i8*, i32)*, void (%struct.JNINativeInterface_**, %struct._jobject*, i16*, i32)*, void (%struct.JNINativeInterface_**, %struct._jobject*, i16*, i32)*, void (%struct.JNINativeInterface_**, %struct._jobject*, i32*, i32)*, void (%struct.JNINativeInterface_**, %struct._jobject*, i64*, i32)*, void (%struct.JNINativeInterface_**, %struct._jobject*, float*, i32)*, void (%struct.JNINativeInterface_**, %struct._jobject*, double*, i32)*, void (%struct.JNINativeInterface_**, %struct._jobject*, i32, i32, i8*)*, void (%struct.JNINativeInterface_**, %struct._jobject*, i32, i32, i8*)*, void (%struct.JNINativeInterface_**, %struct._jobject*, i32, i32, i16*)*, void (%struct.JNINativeInterface_**, %struct._jobject*, i32, i32, i16*)*, void (%struct.JNINativeInterface_**, %struct._jobject*, i32, i32, i32*)*, void (%struct.JNINativeInterface_**, %struct._jobject*, i32, i32, i64*)*, void (%struct.JNINativeInterface_**, %struct._jobject*, i32, i32, float*)*, void (%struct.JNINativeInterface_**, %struct._jobject*, i32, i32, double*)*, void (%struct.JNINativeInterface_**, %struct._jobject*, i32, i32, i8*)*, void (%struct.JNINativeInterface_**, %struct._jobject*, i32, i32, i8*)*, void (%struct.JNINativeInterface_**, %struct._jobject*, i32, i32, i16*)*, void (%struct.JNINativeInterface_**, %struct._jobject*, i32, i32, i16*)*, void (%struct.JNINativeInterface_**, %struct._jobject*, i32, i32, i32*)*, void (%struct.JNINativeInterface_**, %struct._jobject*, i32, i32, i64*)*, void (%struct.JNINativeInterface_**, %struct._jobject*, i32, i32, float*)*, void (%struct.JNINativeInterface_**, %struct._jobject*, i32, i32, double*)*, i32 (%struct.JNINativeInterface_**, %struct._jobject*, %struct.JNINativeMethod*, i32)*, i32 (%struct.JNINativeInterface_**, %struct._jobject*)*, i32 (%struct.JNINativeInterface_**, %struct._jobject*)*, i32 (%struct.JNINativeInterface_**, %struct._jobject*)*, i32 (%struct.JNINativeInterface_**, %struct.JNIInvokeInterface_***)*, void (%struct.JNINativeInterface_**, %struct._jobject*, i32, i32, i16*)*, void (%struct.JNINativeInterface_**, %struct._jobject*, i32, i32, i8*)*, i8* (%struct.JNINativeInterface_**, %struct._jobject*, i8*)*, void (%struct.JNINativeInterface_**, %struct._jobject*, i8*, i32)*, i16* (%struct.JNINativeInterface_**, %struct._jobject*, i8*)*, void (%struct.JNINativeInterface_**, %struct._jobject*, i16*)*, %struct._jobject* (%struct.JNINativeInterface_**, %struct._jobject*)*, void (%struct.JNINativeInterface_**, %struct._jobject*)*, i8 (%struct.JNINativeInterface_**)*, %struct._jobject* (%struct.JNINativeInterface_**, i8*, i64)*, i8* (%struct.JNINativeInterface_**, %struct._jobject*)*, i64 (%struct.JNINativeInterface_**, %struct._jobject*)*, i32 (%struct.JNINativeInterface_**, %struct._jobject*)*, %struct._jobject* (%struct.JNINativeInterface_**, %struct._jobject*)* }
%struct._jmethodID = type opaque
%struct._jfieldID = type opaque
%struct.__va_list_tag = type { i32, i32, i8*, i8* }
%union.jvalue = type { i64 }
%struct.JNINativeMethod = type { i8*, i8*, i8* }
%struct.JNIInvokeInterface_ = type { i8*, i8*, i8*, i32 (%struct.JNIInvokeInterface_**)*, i32 (%struct.JNIInvokeInterface_**, i8**, i8*)*, i32 (%struct.JNIInvokeInterface_**)*, i32 (%struct.JNIInvokeInterface_**, i8**, i32)*, i32 (%struct.JNIInvokeInterface_**, i8**, i8*)* }
%struct._jobject = type opaque

@.str = private unnamed_addr constant [14 x i8] c"indirect_sink\00", align 1
@.str.1 = private unnamed_addr constant [5 x i8] c"(I)V\00", align 1
@.str.2 = private unnamed_addr constant [16 x i8] c"indirect_source\00", align 1
@.str.3 = private unnamed_addr constant [4 x i8] c"()I\00", align 1
@.str.4 = private unnamed_addr constant [18 x i8] c"indirect_sanitize\00", align 1
@.str.5 = private unnamed_addr constant [5 x i8] c"(I)I\00", align 1
@.str.6 = private unnamed_addr constant [11 x i8] c"native %d\0A\00", align 1

; Function Attrs: noinline nounwind optnone uwtable
define dso_local i32 @Java_TaintTest_sum(%struct.JNINativeInterface_** noundef %0, %struct._jobject* noundef %1, i32 noundef %2, i32 noundef %3) #0 {
  %5 = alloca %struct.JNINativeInterface_**, align 8
  %6 = alloca %struct._jobject*, align 8
  %7 = alloca i32, align 4
  %8 = alloca i32, align 4
  store %struct.JNINativeInterface_** %0, %struct.JNINativeInterface_*** %5, align 8
  store %struct._jobject* %1, %struct._jobject** %6, align 8
  store i32 %2, i32* %7, align 4
  store i32 %3, i32* %8, align 4
  %9 = load i32, i32* %7, align 4
  %10 = load i32, i32* %8, align 4
  %11 = add nsw i32 %9, %10
  ret i32 %11
}

; Function Attrs: noinline nounwind optnone uwtable
define dso_local i32 @Java_TaintTest_propagate_1source(%struct.JNINativeInterface_** noundef %0, %struct._jobject* noundef %1) #0 {
  %3 = alloca %struct.JNINativeInterface_**, align 8
  %4 = alloca %struct._jobject*, align 8
  store %struct.JNINativeInterface_** %0, %struct.JNINativeInterface_*** %3, align 8
  store %struct._jobject* %1, %struct._jobject** %4, align 8
  %5 = call i32 @source()
  %6 = add nsw i32 %5, 23
  ret i32 %6
}

; Function Attrs: noinline nounwind optnone uwtable
define dso_local i32 @Java_TaintTest_propagate_1sanitize(%struct.JNINativeInterface_** noundef %0, %struct._jobject* noundef %1, i32 noundef %2) #0 {
  %4 = alloca %struct.JNINativeInterface_**, align 8
  %5 = alloca %struct._jobject*, align 8
  %6 = alloca i32, align 4
  store %struct.JNINativeInterface_** %0, %struct.JNINativeInterface_*** %4, align 8
  store %struct._jobject* %1, %struct._jobject** %5, align 8
  store i32 %2, i32* %6, align 4
  %7 = load i32, i32* %6, align 4
  %8 = call i32 @sanitize(i32 noundef %7)
  ret i32 %8
}

; Function Attrs: noinline nounwind optnone uwtable
define dso_local i32 @sanitize(i32 noundef %0) #0 {
  %2 = alloca i32, align 4
  store i32 %0, i32* %2, align 4
  %3 = load i32, i32* %2, align 4
  %4 = sub nsw i32 %3, 19
  ret i32 %4
}

; Function Attrs: noinline nounwind optnone uwtable
define dso_local i32 @Java_TaintTest_propagate_1sink(%struct.JNINativeInterface_** noundef %0, %struct._jobject* noundef %1, i32 noundef %2) #0 {
  %4 = alloca %struct.JNINativeInterface_**, align 8
  %5 = alloca %struct._jobject*, align 8
  %6 = alloca i32, align 4
  store %struct.JNINativeInterface_** %0, %struct.JNINativeInterface_*** %4, align 8
  store %struct._jobject* %1, %struct._jobject** %5, align 8
  store i32 %2, i32* %6, align 4
  %7 = load i32, i32* %6, align 4
  call void @sink(i32 noundef %7)
  ret i32 23
}

; Function Attrs: noinline nounwind optnone uwtable
define dso_local void @sink(i32 noundef %0) #0 {
  %2 = alloca i32, align 4
  store i32 %0, i32* %2, align 4
  %3 = load i32, i32* %2, align 4
  %4 = call i32 (i8*, ...) @printf(i8* noundef getelementptr inbounds ([11 x i8], [11 x i8]* @.str.6, i64 0, i64 0), i32 noundef %3)
  ret void
}

; Function Attrs: noinline nounwind optnone uwtable
define dso_local i32 @Java_TaintTest_sanitize_1only_1a_1into_1sink(%struct.JNINativeInterface_** noundef %0, %struct._jobject* noundef %1, i32 noundef %2, i32 noundef %3) #0 {
  %5 = alloca %struct.JNINativeInterface_**, align 8
  %6 = alloca %struct._jobject*, align 8
  %7 = alloca i32, align 4
  %8 = alloca i32, align 4
  store %struct.JNINativeInterface_** %0, %struct.JNINativeInterface_*** %5, align 8
  store %struct._jobject* %1, %struct._jobject** %6, align 8
  store i32 %2, i32* %7, align 4
  store i32 %3, i32* %8, align 4
  %9 = load i32, i32* %7, align 4
  %10 = call i32 @sanitize(i32 noundef %9)
  store i32 %10, i32* %7, align 4
  %11 = load i32, i32* %7, align 4
  %12 = load i32, i32* %8, align 4
  %13 = add nsw i32 %11, %12
  call void @sink(i32 noundef %13)
  %14 = load i32, i32* %8, align 4
  ret i32 %14
}

; Function Attrs: noinline nounwind optnone uwtable
define dso_local void @Java_TaintTest_propagate_1identity_1to_1sink(%struct.JNINativeInterface_** noundef %0, %struct._jobject* noundef %1, i32 noundef %2) #0 {
  %4 = alloca %struct.JNINativeInterface_**, align 8
  %5 = alloca %struct._jobject*, align 8
  %6 = alloca i32, align 4
  %7 = alloca i32, align 4
  store %struct.JNINativeInterface_** %0, %struct.JNINativeInterface_*** %4, align 8
  store %struct._jobject* %1, %struct._jobject** %5, align 8
  store i32 %2, i32* %6, align 4
  %8 = load i32, i32* %6, align 4
  %9 = call i32 @identity(i32 noundef %8)
  store i32 %9, i32* %7, align 4
  %10 = load i32, i32* %7, align 4
  call void @sink(i32 noundef %10)
  ret void
}

; Function Attrs: noinline nounwind optnone uwtable
define dso_local i32 @identity(i32 noundef %0) #0 {
  %2 = alloca i32, align 4
  store i32 %0, i32* %2, align 4
  %3 = load i32, i32* %2, align 4
  ret i32 %3
}

; Function Attrs: noinline nounwind optnone uwtable
define dso_local void @Java_TaintTest_propagate_1zero_1to_1sink(%struct.JNINativeInterface_** noundef %0, %struct._jobject* noundef %1, i32 noundef %2) #0 {
  %4 = alloca %struct.JNINativeInterface_**, align 8
  %5 = alloca %struct._jobject*, align 8
  %6 = alloca i32, align 4
  %7 = alloca i32, align 4
  store %struct.JNINativeInterface_** %0, %struct.JNINativeInterface_*** %4, align 8
  store %struct._jobject* %1, %struct._jobject** %5, align 8
  store i32 %2, i32* %6, align 4
  %8 = load i32, i32* %6, align 4
  %9 = call i32 @zero(i32 noundef %8)
  store i32 %9, i32* %7, align 4
  %10 = load i32, i32* %7, align 4
  call void @sink(i32 noundef %10)
  ret void
}

; Function Attrs: noinline nounwind optnone uwtable
define dso_local i32 @zero(i32 noundef %0) #0 {
  %2 = alloca i32, align 4
  store i32 %0, i32* %2, align 4
  ret i32 0
}

; Function Attrs: noinline nounwind optnone uwtable
define dso_local void @Java_TaintTest_native_1array_1tainted(%struct.JNINativeInterface_** noundef %0, %struct._jobject* noundef %1) #0 {
  %3 = alloca %struct.JNINativeInterface_**, align 8
  %4 = alloca %struct._jobject*, align 8
  %5 = alloca [2 x i32], align 4
  store %struct.JNINativeInterface_** %0, %struct.JNINativeInterface_*** %3, align 8
  store %struct._jobject* %1, %struct._jobject** %4, align 8
  %6 = bitcast [2 x i32]* %5 to i8*
  call void @llvm.memset.p0i8.i64(i8* align 4 %6, i8 0, i64 8, i1 false)
  %7 = call i32 @source()
  %8 = getelementptr inbounds [2 x i32], [2 x i32]* %5, i64 0, i64 1
  store i32 %7, i32* %8, align 4
  %9 = getelementptr inbounds [2 x i32], [2 x i32]* %5, i64 0, i64 1
  %10 = load i32, i32* %9, align 4
  call void @sink(i32 noundef %10)
  ret void
}

; Function Attrs: argmemonly nofree nounwind willreturn writeonly
declare void @llvm.memset.p0i8.i64(i8* nocapture writeonly, i8, i64, i1 immarg) #1

; Function Attrs: noinline nounwind optnone uwtable
define dso_local void @Java_TaintTest_native_1array_1untainted(%struct.JNINativeInterface_** noundef %0, %struct._jobject* noundef %1) #0 {
  %3 = alloca %struct.JNINativeInterface_**, align 8
  %4 = alloca %struct._jobject*, align 8
  %5 = alloca [2 x i32], align 4
  store %struct.JNINativeInterface_** %0, %struct.JNINativeInterface_*** %3, align 8
  store %struct._jobject* %1, %struct._jobject** %4, align 8
  %6 = bitcast [2 x i32]* %5 to i8*
  call void @llvm.memset.p0i8.i64(i8* align 4 %6, i8 0, i64 8, i1 false)
  %7 = call i32 @source()
  %8 = getelementptr inbounds [2 x i32], [2 x i32]* %5, i64 0, i64 0
  store i32 %7, i32* %8, align 4
  %9 = getelementptr inbounds [2 x i32], [2 x i32]* %5, i64 0, i64 1
  %10 = load i32, i32* %9, align 4
  call void @sink(i32 noundef %10)
  ret void
}

; Function Attrs: noinline nounwind optnone uwtable
define dso_local void @Java_TaintTest_propagate_1to_1java_1sink(%struct.JNINativeInterface_** noundef %0, %struct._jobject* noundef %1, i32 noundef %2) #0 {
  %4 = alloca %struct.JNINativeInterface_**, align 8
  %5 = alloca %struct._jobject*, align 8
  %6 = alloca i32, align 4
  %7 = alloca %struct._jobject*, align 8
  %8 = alloca %struct._jmethodID*, align 8
  store %struct.JNINativeInterface_** %0, %struct.JNINativeInterface_*** %4, align 8
  store %struct._jobject* %1, %struct._jobject** %5, align 8
  store i32 %2, i32* %6, align 4
  %9 = load %struct.JNINativeInterface_**, %struct.JNINativeInterface_*** %4, align 8
  %10 = load %struct.JNINativeInterface_*, %struct.JNINativeInterface_** %9, align 8
  %11 = getelementptr inbounds %struct.JNINativeInterface_, %struct.JNINativeInterface_* %10, i32 0, i32 31
  %12 = load %struct._jobject* (%struct.JNINativeInterface_**, %struct._jobject*)*, %struct._jobject* (%struct.JNINativeInterface_**, %struct._jobject*)** %11, align 8
  %13 = load %struct.JNINativeInterface_**, %struct.JNINativeInterface_*** %4, align 8
  %14 = load %struct._jobject*, %struct._jobject** %5, align 8
  %15 = call %struct._jobject* %12(%struct.JNINativeInterface_** noundef %13, %struct._jobject* noundef %14)
  store %struct._jobject* %15, %struct._jobject** %7, align 8
  %16 = load %struct.JNINativeInterface_**, %struct.JNINativeInterface_*** %4, align 8
  %17 = load %struct.JNINativeInterface_*, %struct.JNINativeInterface_** %16, align 8
  %18 = getelementptr inbounds %struct.JNINativeInterface_, %struct.JNINativeInterface_* %17, i32 0, i32 33
  %19 = load %struct._jmethodID* (%struct.JNINativeInterface_**, %struct._jobject*, i8*, i8*)*, %struct._jmethodID* (%struct.JNINativeInterface_**, %struct._jobject*, i8*, i8*)** %18, align 8
  %20 = load %struct.JNINativeInterface_**, %struct.JNINativeInterface_*** %4, align 8
  %21 = load %struct._jobject*, %struct._jobject** %7, align 8
  %22 = call %struct._jmethodID* %19(%struct.JNINativeInterface_** noundef %20, %struct._jobject* noundef %21, i8* noundef getelementptr inbounds ([14 x i8], [14 x i8]* @.str, i64 0, i64 0), i8* noundef getelementptr inbounds ([5 x i8], [5 x i8]* @.str.1, i64 0, i64 0))
  store %struct._jmethodID* %22, %struct._jmethodID** %8, align 8
  %23 = load %struct.JNINativeInterface_**, %struct.JNINativeInterface_*** %4, align 8
  %24 = load %struct.JNINativeInterface_*, %struct.JNINativeInterface_** %23, align 8
  %25 = getelementptr inbounds %struct.JNINativeInterface_, %struct.JNINativeInterface_* %24, i32 0, i32 61
  %26 = load void (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, ...)*, void (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, ...)** %25, align 8
  %27 = load %struct.JNINativeInterface_**, %struct.JNINativeInterface_*** %4, align 8
  %28 = load %struct._jobject*, %struct._jobject** %5, align 8
  %29 = load %struct._jmethodID*, %struct._jmethodID** %8, align 8
  %30 = load i32, i32* %6, align 4
  call void (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, ...) %26(%struct.JNINativeInterface_** noundef %27, %struct._jobject* noundef %28, %struct._jmethodID* noundef %29, i32 noundef %30)
  ret void
}

; Function Attrs: noinline nounwind optnone uwtable
define dso_local i32 @Java_TaintTest_propagate_1from_1java_1source(%struct.JNINativeInterface_** noundef %0, %struct._jobject* noundef %1) #0 {
  %3 = alloca %struct.JNINativeInterface_**, align 8
  %4 = alloca %struct._jobject*, align 8
  %5 = alloca %struct._jobject*, align 8
  %6 = alloca %struct._jmethodID*, align 8
  store %struct.JNINativeInterface_** %0, %struct.JNINativeInterface_*** %3, align 8
  store %struct._jobject* %1, %struct._jobject** %4, align 8
  %7 = load %struct.JNINativeInterface_**, %struct.JNINativeInterface_*** %3, align 8
  %8 = load %struct.JNINativeInterface_*, %struct.JNINativeInterface_** %7, align 8
  %9 = getelementptr inbounds %struct.JNINativeInterface_, %struct.JNINativeInterface_* %8, i32 0, i32 31
  %10 = load %struct._jobject* (%struct.JNINativeInterface_**, %struct._jobject*)*, %struct._jobject* (%struct.JNINativeInterface_**, %struct._jobject*)** %9, align 8
  %11 = load %struct.JNINativeInterface_**, %struct.JNINativeInterface_*** %3, align 8
  %12 = load %struct._jobject*, %struct._jobject** %4, align 8
  %13 = call %struct._jobject* %10(%struct.JNINativeInterface_** noundef %11, %struct._jobject* noundef %12)
  store %struct._jobject* %13, %struct._jobject** %5, align 8
  %14 = load %struct.JNINativeInterface_**, %struct.JNINativeInterface_*** %3, align 8
  %15 = load %struct.JNINativeInterface_*, %struct.JNINativeInterface_** %14, align 8
  %16 = getelementptr inbounds %struct.JNINativeInterface_, %struct.JNINativeInterface_* %15, i32 0, i32 33
  %17 = load %struct._jmethodID* (%struct.JNINativeInterface_**, %struct._jobject*, i8*, i8*)*, %struct._jmethodID* (%struct.JNINativeInterface_**, %struct._jobject*, i8*, i8*)** %16, align 8
  %18 = load %struct.JNINativeInterface_**, %struct.JNINativeInterface_*** %3, align 8
  %19 = load %struct._jobject*, %struct._jobject** %5, align 8
  %20 = call %struct._jmethodID* %17(%struct.JNINativeInterface_** noundef %18, %struct._jobject* noundef %19, i8* noundef getelementptr inbounds ([16 x i8], [16 x i8]* @.str.2, i64 0, i64 0), i8* noundef getelementptr inbounds ([4 x i8], [4 x i8]* @.str.3, i64 0, i64 0))
  store %struct._jmethodID* %20, %struct._jmethodID** %6, align 8
  %21 = load %struct.JNINativeInterface_**, %struct.JNINativeInterface_*** %3, align 8
  %22 = load %struct.JNINativeInterface_*, %struct.JNINativeInterface_** %21, align 8
  %23 = getelementptr inbounds %struct.JNINativeInterface_, %struct.JNINativeInterface_* %22, i32 0, i32 49
  %24 = load i32 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, ...)*, i32 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, ...)** %23, align 8
  %25 = load %struct.JNINativeInterface_**, %struct.JNINativeInterface_*** %3, align 8
  %26 = load %struct._jobject*, %struct._jobject** %4, align 8
  %27 = load %struct._jmethodID*, %struct._jmethodID** %6, align 8
  %28 = call i32 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, ...) %24(%struct.JNINativeInterface_** noundef %25, %struct._jobject* noundef %26, %struct._jmethodID* noundef %27)
  ret i32 %28
}

; Function Attrs: noinline nounwind optnone uwtable
define dso_local i32 @Java_TaintTest_propagate_1java_1sanitize(%struct.JNINativeInterface_** noundef %0, %struct._jobject* noundef %1, i32 noundef %2) #0 {
  %4 = alloca %struct.JNINativeInterface_**, align 8
  %5 = alloca %struct._jobject*, align 8
  %6 = alloca i32, align 4
  %7 = alloca %struct._jobject*, align 8
  %8 = alloca %struct._jmethodID*, align 8
  store %struct.JNINativeInterface_** %0, %struct.JNINativeInterface_*** %4, align 8
  store %struct._jobject* %1, %struct._jobject** %5, align 8
  store i32 %2, i32* %6, align 4
  %9 = load %struct.JNINativeInterface_**, %struct.JNINativeInterface_*** %4, align 8
  %10 = load %struct.JNINativeInterface_*, %struct.JNINativeInterface_** %9, align 8
  %11 = getelementptr inbounds %struct.JNINativeInterface_, %struct.JNINativeInterface_* %10, i32 0, i32 31
  %12 = load %struct._jobject* (%struct.JNINativeInterface_**, %struct._jobject*)*, %struct._jobject* (%struct.JNINativeInterface_**, %struct._jobject*)** %11, align 8
  %13 = load %struct.JNINativeInterface_**, %struct.JNINativeInterface_*** %4, align 8
  %14 = load %struct._jobject*, %struct._jobject** %5, align 8
  %15 = call %struct._jobject* %12(%struct.JNINativeInterface_** noundef %13, %struct._jobject* noundef %14)
  store %struct._jobject* %15, %struct._jobject** %7, align 8
  %16 = load %struct.JNINativeInterface_**, %struct.JNINativeInterface_*** %4, align 8
  %17 = load %struct.JNINativeInterface_*, %struct.JNINativeInterface_** %16, align 8
  %18 = getelementptr inbounds %struct.JNINativeInterface_, %struct.JNINativeInterface_* %17, i32 0, i32 33
  %19 = load %struct._jmethodID* (%struct.JNINativeInterface_**, %struct._jobject*, i8*, i8*)*, %struct._jmethodID* (%struct.JNINativeInterface_**, %struct._jobject*, i8*, i8*)** %18, align 8
  %20 = load %struct.JNINativeInterface_**, %struct.JNINativeInterface_*** %4, align 8
  %21 = load %struct._jobject*, %struct._jobject** %7, align 8
  %22 = call %struct._jmethodID* %19(%struct.JNINativeInterface_** noundef %20, %struct._jobject* noundef %21, i8* noundef getelementptr inbounds ([18 x i8], [18 x i8]* @.str.4, i64 0, i64 0), i8* noundef getelementptr inbounds ([5 x i8], [5 x i8]* @.str.5, i64 0, i64 0))
  store %struct._jmethodID* %22, %struct._jmethodID** %8, align 8
  %23 = load %struct.JNINativeInterface_**, %struct.JNINativeInterface_*** %4, align 8
  %24 = load %struct.JNINativeInterface_*, %struct.JNINativeInterface_** %23, align 8
  %25 = getelementptr inbounds %struct.JNINativeInterface_, %struct.JNINativeInterface_* %24, i32 0, i32 49
  %26 = load i32 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, ...)*, i32 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, ...)** %25, align 8
  %27 = load %struct.JNINativeInterface_**, %struct.JNINativeInterface_*** %4, align 8
  %28 = load %struct._jobject*, %struct._jobject** %5, align 8
  %29 = load %struct._jmethodID*, %struct._jmethodID** %8, align 8
  %30 = load i32, i32* %6, align 4
  %31 = call i32 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, ...) %26(%struct.JNINativeInterface_** noundef %27, %struct._jobject* noundef %28, %struct._jmethodID* noundef %29, i32 noundef %30)
  ret i32 %31
}

; Function Attrs: noinline nounwind optnone uwtable
define dso_local i32 @source() #0 {
  ret i32 42
}

declare i32 @printf(i8* noundef, ...) #2

attributes #0 = { noinline nounwind optnone uwtable "frame-pointer"="all" "min-legal-vector-width"="0" "no-trapping-math"="true" "stack-protector-buffer-size"="8" "target-cpu"="x86-64" "target-features"="+cx8,+fxsr,+mmx,+sse,+sse2,+x87" "tune-cpu"="generic" }
attributes #1 = { argmemonly nofree nounwind willreturn writeonly }
attributes #2 = { "frame-pointer"="all" "no-trapping-math"="true" "stack-protector-buffer-size"="8" "target-cpu"="x86-64" "target-features"="+cx8,+fxsr,+mmx,+sse,+sse2,+x87" "tune-cpu"="generic" }

!llvm.module.flags = !{!0, !1, !2, !3, !4}
!llvm.ident = !{!5}

!0 = !{i32 1, !"wchar_size", i32 4}
!1 = !{i32 7, !"PIC Level", i32 2}
!2 = !{i32 7, !"PIE Level", i32 2}
!3 = !{i32 7, !"uwtable", i32 1}
!4 = !{i32 7, !"frame-pointer", i32 2}
!5 = !{!"Ubuntu clang version 14.0.0-1ubuntu1"}
