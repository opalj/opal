; ModuleID = '<stdin>'
source_filename = "native.c"
target datalayout = "e-m:e-p270:32:32-p271:32:32-p272:64:64-i64:64-f80:128-n8:16:32:64-S128"
target triple = "x86_64-unknown-linux-gnu"

%struct.JNINativeInterface_ = type { i8*, i8*, i8*, i8*, i32 (%struct.JNINativeInterface_**)*, %struct._jobject* (%struct.JNINativeInterface_**, i8*, %struct._jobject*, i8*, i32)*, %struct._jobject* (%struct.JNINativeInterface_**, i8*)*, %struct._jmethodID* (%struct.JNINativeInterface_**, %struct._jobject*)*, %struct._jfieldID* (%struct.JNINativeInterface_**, %struct._jobject*)*, %struct._jobject* (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, i8)*, %struct._jobject* (%struct.JNINativeInterface_**, %struct._jobject*)*, i8 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jobject*)*, %struct._jobject* (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*, i8)*, i32 (%struct.JNINativeInterface_**, %struct._jobject*)*, i32 (%struct.JNINativeInterface_**, %struct._jobject*, i8*)*, %struct._jobject* (%struct.JNINativeInterface_**)*, void (%struct.JNINativeInterface_**)*, void (%struct.JNINativeInterface_**)*, void (%struct.JNINativeInterface_**, i8*)*, i32 (%struct.JNINativeInterface_**, i32)*, %struct._jobject* (%struct.JNINativeInterface_**, %struct._jobject*)*, %struct._jobject* (%struct.JNINativeInterface_**, %struct._jobject*)*, void (%struct.JNINativeInterface_**, %struct._jobject*)*, void (%struct.JNINativeInterface_**, %struct._jobject*)*, i8 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jobject*)*, %struct._jobject* (%struct.JNINativeInterface_**, %struct._jobject*)*, i32 (%struct.JNINativeInterface_**, i32)*, %struct._jobject* (%struct.JNINativeInterface_**, %struct._jobject*)*, %struct._jobject* (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, ...)*, %struct._jobject* (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %struct.__va_list_tag*)*, %struct._jobject* (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %union.jvalue*)*, %struct._jobject* (%struct.JNINativeInterface_**, %struct._jobject*)*, i8 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jobject*)*, %struct._jmethodID* (%struct.JNINativeInterface_**, %struct._jobject*, i8*, i8*)*, %struct._jobject* (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, ...)*, %struct._jobject* (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %struct.__va_list_tag*)*, %struct._jobject* (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %union.jvalue*)*, i8 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, ...)*, i8 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %struct.__va_list_tag*)*, i8 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %union.jvalue*)*, i8 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, ...)*, i8 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %struct.__va_list_tag*)*, i8 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %union.jvalue*)*, i16 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, ...)*, i16 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %struct.__va_list_tag*)*, i16 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %union.jvalue*)*, i16 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, ...)*, i16 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %struct.__va_list_tag*)*, i16 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %union.jvalue*)*, i32 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, ...)*, i32 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %struct.__va_list_tag*)*, i32 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %union.jvalue*)*, i64 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, ...)*, i64 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %struct.__va_list_tag*)*, i64 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %union.jvalue*)*, float (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, ...)*, float (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %struct.__va_list_tag*)*, float (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %union.jvalue*)*, double (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, ...)*, double (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %struct.__va_list_tag*)*, double (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %union.jvalue*)*, void (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, ...)*, void (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %struct.__va_list_tag*)*, void (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %union.jvalue*)*, %struct._jobject* (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jobject*, %struct._jmethodID*, ...)*, %struct._jobject* (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jobject*, %struct._jmethodID*, %struct.__va_list_tag*)*, %struct._jobject* (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jobject*, %struct._jmethodID*, %union.jvalue*)*, i8 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jobject*, %struct._jmethodID*, ...)*, i8 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jobject*, %struct._jmethodID*, %struct.__va_list_tag*)*, i8 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jobject*, %struct._jmethodID*, %union.jvalue*)*, i8 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jobject*, %struct._jmethodID*, ...)*, i8 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jobject*, %struct._jmethodID*, %struct.__va_list_tag*)*, i8 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jobject*, %struct._jmethodID*, %union.jvalue*)*, i16 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jobject*, %struct._jmethodID*, ...)*, i16 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jobject*, %struct._jmethodID*, %struct.__va_list_tag*)*, i16 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jobject*, %struct._jmethodID*, %union.jvalue*)*, i16 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jobject*, %struct._jmethodID*, ...)*, i16 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jobject*, %struct._jmethodID*, %struct.__va_list_tag*)*, i16 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jobject*, %struct._jmethodID*, %union.jvalue*)*, i32 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jobject*, %struct._jmethodID*, ...)*, i32 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jobject*, %struct._jmethodID*, %struct.__va_list_tag*)*, i32 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jobject*, %struct._jmethodID*, %union.jvalue*)*, i64 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jobject*, %struct._jmethodID*, ...)*, i64 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jobject*, %struct._jmethodID*, %struct.__va_list_tag*)*, i64 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jobject*, %struct._jmethodID*, %union.jvalue*)*, float (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jobject*, %struct._jmethodID*, ...)*, float (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jobject*, %struct._jmethodID*, %struct.__va_list_tag*)*, float (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jobject*, %struct._jmethodID*, %union.jvalue*)*, double (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jobject*, %struct._jmethodID*, ...)*, double (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jobject*, %struct._jmethodID*, %struct.__va_list_tag*)*, double (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jobject*, %struct._jmethodID*, %union.jvalue*)*, void (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jobject*, %struct._jmethodID*, ...)*, void (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jobject*, %struct._jmethodID*, %struct.__va_list_tag*)*, void (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jobject*, %struct._jmethodID*, %union.jvalue*)*, %struct._jfieldID* (%struct.JNINativeInterface_**, %struct._jobject*, i8*, i8*)*, %struct._jobject* (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*)*, i8 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*)*, i8 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*)*, i16 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*)*, i16 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*)*, i32 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*)*, i64 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*)*, float (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*)*, double (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*)*, void (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*, %struct._jobject*)*, void (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*, i8)*, void (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*, i8)*, void (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*, i16)*, void (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*, i16)*, void (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*, i32)*, void (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*, i64)*, void (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*, float)*, void (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*, double)*, %struct._jmethodID* (%struct.JNINativeInterface_**, %struct._jobject*, i8*, i8*)*, %struct._jobject* (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, ...)*, %struct._jobject* (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %struct.__va_list_tag*)*, %struct._jobject* (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %union.jvalue*)*, i8 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, ...)*, i8 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %struct.__va_list_tag*)*, i8 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %union.jvalue*)*, i8 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, ...)*, i8 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %struct.__va_list_tag*)*, i8 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %union.jvalue*)*, i16 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, ...)*, i16 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %struct.__va_list_tag*)*, i16 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %union.jvalue*)*, i16 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, ...)*, i16 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %struct.__va_list_tag*)*, i16 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %union.jvalue*)*, i32 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, ...)*, i32 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %struct.__va_list_tag*)*, i32 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %union.jvalue*)*, i64 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, ...)*, i64 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %struct.__va_list_tag*)*, i64 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %union.jvalue*)*, float (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, ...)*, float (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %struct.__va_list_tag*)*, float (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %union.jvalue*)*, double (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, ...)*, double (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %struct.__va_list_tag*)*, double (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %union.jvalue*)*, void (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, ...)*, void (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %struct.__va_list_tag*)*, void (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, %union.jvalue*)*, %struct._jfieldID* (%struct.JNINativeInterface_**, %struct._jobject*, i8*, i8*)*, %struct._jobject* (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*)*, i8 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*)*, i8 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*)*, i16 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*)*, i16 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*)*, i32 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*)*, i64 (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*)*, float (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*)*, double (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*)*, void (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*, %struct._jobject*)*, void (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*, i8)*, void (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*, i8)*, void (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*, i16)*, void (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*, i16)*, void (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*, i32)*, void (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*, i64)*, void (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*, float)*, void (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*, double)*, %struct._jobject* (%struct.JNINativeInterface_**, i16*, i32)*, i32 (%struct.JNINativeInterface_**, %struct._jobject*)*, i16* (%struct.JNINativeInterface_**, %struct._jobject*, i8*)*, void (%struct.JNINativeInterface_**, %struct._jobject*, i16*)*, %struct._jobject* (%struct.JNINativeInterface_**, i8*)*, i32 (%struct.JNINativeInterface_**, %struct._jobject*)*, i8* (%struct.JNINativeInterface_**, %struct._jobject*, i8*)*, void (%struct.JNINativeInterface_**, %struct._jobject*, i8*)*, i32 (%struct.JNINativeInterface_**, %struct._jobject*)*, %struct._jobject* (%struct.JNINativeInterface_**, i32, %struct._jobject*, %struct._jobject*)*, %struct._jobject* (%struct.JNINativeInterface_**, %struct._jobject*, i32)*, void (%struct.JNINativeInterface_**, %struct._jobject*, i32, %struct._jobject*)*, %struct._jobject* (%struct.JNINativeInterface_**, i32)*, %struct._jobject* (%struct.JNINativeInterface_**, i32)*, %struct._jobject* (%struct.JNINativeInterface_**, i32)*, %struct._jobject* (%struct.JNINativeInterface_**, i32)*, %struct._jobject* (%struct.JNINativeInterface_**, i32)*, %struct._jobject* (%struct.JNINativeInterface_**, i32)*, %struct._jobject* (%struct.JNINativeInterface_**, i32)*, %struct._jobject* (%struct.JNINativeInterface_**, i32)*, i8* (%struct.JNINativeInterface_**, %struct._jobject*, i8*)*, i8* (%struct.JNINativeInterface_**, %struct._jobject*, i8*)*, i16* (%struct.JNINativeInterface_**, %struct._jobject*, i8*)*, i16* (%struct.JNINativeInterface_**, %struct._jobject*, i8*)*, i32* (%struct.JNINativeInterface_**, %struct._jobject*, i8*)*, i64* (%struct.JNINativeInterface_**, %struct._jobject*, i8*)*, float* (%struct.JNINativeInterface_**, %struct._jobject*, i8*)*, double* (%struct.JNINativeInterface_**, %struct._jobject*, i8*)*, void (%struct.JNINativeInterface_**, %struct._jobject*, i8*, i32)*, void (%struct.JNINativeInterface_**, %struct._jobject*, i8*, i32)*, void (%struct.JNINativeInterface_**, %struct._jobject*, i16*, i32)*, void (%struct.JNINativeInterface_**, %struct._jobject*, i16*, i32)*, void (%struct.JNINativeInterface_**, %struct._jobject*, i32*, i32)*, void (%struct.JNINativeInterface_**, %struct._jobject*, i64*, i32)*, void (%struct.JNINativeInterface_**, %struct._jobject*, float*, i32)*, void (%struct.JNINativeInterface_**, %struct._jobject*, double*, i32)*, void (%struct.JNINativeInterface_**, %struct._jobject*, i32, i32, i8*)*, void (%struct.JNINativeInterface_**, %struct._jobject*, i32, i32, i8*)*, void (%struct.JNINativeInterface_**, %struct._jobject*, i32, i32, i16*)*, void (%struct.JNINativeInterface_**, %struct._jobject*, i32, i32, i16*)*, void (%struct.JNINativeInterface_**, %struct._jobject*, i32, i32, i32*)*, void (%struct.JNINativeInterface_**, %struct._jobject*, i32, i32, i64*)*, void (%struct.JNINativeInterface_**, %struct._jobject*, i32, i32, float*)*, void (%struct.JNINativeInterface_**, %struct._jobject*, i32, i32, double*)*, void (%struct.JNINativeInterface_**, %struct._jobject*, i32, i32, i8*)*, void (%struct.JNINativeInterface_**, %struct._jobject*, i32, i32, i8*)*, void (%struct.JNINativeInterface_**, %struct._jobject*, i32, i32, i16*)*, void (%struct.JNINativeInterface_**, %struct._jobject*, i32, i32, i16*)*, void (%struct.JNINativeInterface_**, %struct._jobject*, i32, i32, i32*)*, void (%struct.JNINativeInterface_**, %struct._jobject*, i32, i32, i64*)*, void (%struct.JNINativeInterface_**, %struct._jobject*, i32, i32, float*)*, void (%struct.JNINativeInterface_**, %struct._jobject*, i32, i32, double*)*, i32 (%struct.JNINativeInterface_**, %struct._jobject*, %struct.JNINativeMethod*, i32)*, i32 (%struct.JNINativeInterface_**, %struct._jobject*)*, i32 (%struct.JNINativeInterface_**, %struct._jobject*)*, i32 (%struct.JNINativeInterface_**, %struct._jobject*)*, i32 (%struct.JNINativeInterface_**, %struct.JNIInvokeInterface_***)*, void (%struct.JNINativeInterface_**, %struct._jobject*, i32, i32, i16*)*, void (%struct.JNINativeInterface_**, %struct._jobject*, i32, i32, i8*)*, i8* (%struct.JNINativeInterface_**, %struct._jobject*, i8*)*, void (%struct.JNINativeInterface_**, %struct._jobject*, i8*, i32)*, i16* (%struct.JNINativeInterface_**, %struct._jobject*, i8*)*, void (%struct.JNINativeInterface_**, %struct._jobject*, i16*)*, %struct._jobject* (%struct.JNINativeInterface_**, %struct._jobject*)*, void (%struct.JNINativeInterface_**, %struct._jobject*)*, i8 (%struct.JNINativeInterface_**)*, %struct._jobject* (%struct.JNINativeInterface_**, i8*, i64)*, i8* (%struct.JNINativeInterface_**, %struct._jobject*)*, i64 (%struct.JNINativeInterface_**, %struct._jobject*)*, i32 (%struct.JNINativeInterface_**, %struct._jobject*)*, %struct._jobject* (%struct.JNINativeInterface_**, %struct._jobject*)* }
%struct._jmethodID = type opaque
%struct._jfieldID = type opaque
%struct.__va_list_tag = type { i32, i32, i8*, i8* }
%union.jvalue = type { i64 }
%struct.JNINativeMethod = type { i8*, i8*, i8* }
%struct.JNIInvokeInterface_ = type { i8*, i8*, i8*, i32 (%struct.JNIInvokeInterface_**)*, i32 (%struct.JNIInvokeInterface_**, i8**, i8*)*, i32 (%struct.JNIInvokeInterface_**)*, i32 (%struct.JNIInvokeInterface_**, i8**, i32)*, i32 (%struct.JNIInvokeInterface_**, i8**, i8*)* }
%struct._jobject = type opaque

@.str = private unnamed_addr constant [87 x i8] c"Lorg/opalj/fpcf/fixtures/xl/llvm/controlflow/bidirectional/CallJavaFunctionFromNative;\00", align 1
@.str.1 = private unnamed_addr constant [15 x i8] c"myJavaFunction\00", align 1
@.str.2 = private unnamed_addr constant [22 x i8] c"(Ljava/lang/Object;)V\00", align 1
@.str.3 = private unnamed_addr constant [89 x i8] c"Lorg/opalj/fpcf/fixtures/xl/llvm/controlflow/bidirectional/CreateJavaInstanceFromNative;\00", align 1
@.str.4 = private unnamed_addr constant [7 x i8] c"<init>\00", align 1
@.str.5 = private unnamed_addr constant [4 x i8] c"()V\00", align 1
@.str.6 = private unnamed_addr constant [96 x i8] c"Lorg/opalj/fpcf/fixtures/xl/llvm/controlflow/bidirectional/CallJavaFunctionFromNativeAndReturn;\00", align 1
@.str.7 = private unnamed_addr constant [24 x i8] c"myReturningJavaFunction\00", align 1
@.str.8 = private unnamed_addr constant [39 x i8] c"(Ljava/lang/Object;)Ljava/lang/Object;\00", align 1
@.str.9 = private unnamed_addr constant [86 x i8] c"Lorg/opalj/fpcf/fixtures/xl/llvm/stateaccess/unidirectional/WriteJavaFieldFromNative;\00", align 1
@.str.10 = private unnamed_addr constant [8 x i8] c"myfield\00", align 1
@.str.11 = private unnamed_addr constant [19 x i8] c"Ljava/lang/Object;\00", align 1
@.str.12 = private unnamed_addr constant [85 x i8] c"Lorg/opalj/fpcf/fixtures/xl/llvm/stateaccess/unidirectional/ReadJavaFieldFromNative;\00", align 1
@globb = global i8* null, align 8
@.str.13 = private unnamed_addr constant [9 x i8] c"asdasdas\00", align 1
@.str.14 = private unnamed_addr constant [7 x i8] c"XYXYXY\00", align 1

; Function Attrs: noinline nounwind optnone uwtable
define void @Java_org_opalj_fpcf_fixtures_xl_llvm_controlflow_bidirectional_CallJavaFunctionFromNative_callMyJavaFunctionFromNative(%struct.JNINativeInterface_** noundef %0, %struct._jobject* noundef %1, %struct._jobject* noundef %2) #0 {
  %4 = alloca %struct.JNINativeInterface_**, align 8
  %5 = alloca %struct._jobject*, align 8
  %6 = alloca %struct._jobject*, align 8
  %7 = alloca %struct._jobject*, align 8
  %8 = alloca %struct._jmethodID*, align 8
  store %struct.JNINativeInterface_** %0, %struct.JNINativeInterface_*** %4, align 8
  store %struct._jobject* %1, %struct._jobject** %5, align 8
  store %struct._jobject* %2, %struct._jobject** %6, align 8
  %9 = load %struct.JNINativeInterface_**, %struct.JNINativeInterface_*** %4, align 8
  %10 = load %struct.JNINativeInterface_*, %struct.JNINativeInterface_** %9, align 8
  %11 = getelementptr inbounds %struct.JNINativeInterface_, %struct.JNINativeInterface_* %10, i32 0, i32 6
  %12 = load %struct._jobject* (%struct.JNINativeInterface_**, i8*)*, %struct._jobject* (%struct.JNINativeInterface_**, i8*)** %11, align 8
  %13 = load %struct.JNINativeInterface_**, %struct.JNINativeInterface_*** %4, align 8
  %14 = call %struct._jobject* %12(%struct.JNINativeInterface_** noundef %13, i8* noundef getelementptr inbounds ([87 x i8], [87 x i8]* @.str, i64 0, i64 0))
  store %struct._jobject* %14, %struct._jobject** %7, align 8
  %15 = load %struct.JNINativeInterface_**, %struct.JNINativeInterface_*** %4, align 8
  %16 = load %struct.JNINativeInterface_*, %struct.JNINativeInterface_** %15, align 8
  %17 = getelementptr inbounds %struct.JNINativeInterface_, %struct.JNINativeInterface_* %16, i32 0, i32 33
  %18 = load %struct._jmethodID* (%struct.JNINativeInterface_**, %struct._jobject*, i8*, i8*)*, %struct._jmethodID* (%struct.JNINativeInterface_**, %struct._jobject*, i8*, i8*)** %17, align 8
  %19 = load %struct.JNINativeInterface_**, %struct.JNINativeInterface_*** %4, align 8
  %20 = load %struct._jobject*, %struct._jobject** %7, align 8
  %21 = call %struct._jmethodID* %18(%struct.JNINativeInterface_** noundef %19, %struct._jobject* noundef %20, i8* noundef getelementptr inbounds ([15 x i8], [15 x i8]* @.str.1, i64 0, i64 0), i8* noundef getelementptr inbounds ([22 x i8], [22 x i8]* @.str.2, i64 0, i64 0))
  store %struct._jmethodID* %21, %struct._jmethodID** %8, align 8
  %22 = load %struct.JNINativeInterface_**, %struct.JNINativeInterface_*** %4, align 8
  %23 = load %struct.JNINativeInterface_*, %struct.JNINativeInterface_** %22, align 8
  %24 = getelementptr inbounds %struct.JNINativeInterface_, %struct.JNINativeInterface_* %23, i32 0, i32 61
  %25 = load void (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, ...)*, void (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, ...)** %24, align 8
  %26 = load %struct.JNINativeInterface_**, %struct.JNINativeInterface_*** %4, align 8
  %27 = load %struct._jobject*, %struct._jobject** %5, align 8
  %28 = load %struct._jmethodID*, %struct._jmethodID** %8, align 8
  %29 = load %struct._jobject*, %struct._jobject** %6, align 8
  call void (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, ...) %25(%struct.JNINativeInterface_** noundef %26, %struct._jobject* noundef %27, %struct._jmethodID* noundef %28, %struct._jobject* noundef %29)
  ret void
}

; Function Attrs: noinline nounwind optnone uwtable
define void @Java_org_opalj_fpcf_fixtures_xl_llvm_controlflow_bidirectional_CreateJavaInstanceFromNative_createInstanceAndCallMyFunctionFromNative(%struct.JNINativeInterface_** noundef %0, %struct._jobject* noundef %1, %struct._jobject* noundef %2) #0 {
  %4 = alloca %struct.JNINativeInterface_**, align 8
  %5 = alloca %struct._jobject*, align 8
  %6 = alloca %struct._jobject*, align 8
  %7 = alloca %struct._jobject*, align 8
  %8 = alloca %struct._jmethodID*, align 8
  %9 = alloca %struct._jobject*, align 8
  %10 = alloca %struct._jmethodID*, align 8
  store %struct.JNINativeInterface_** %0, %struct.JNINativeInterface_*** %4, align 8
  store %struct._jobject* %1, %struct._jobject** %5, align 8
  store %struct._jobject* %2, %struct._jobject** %6, align 8
  %11 = load %struct.JNINativeInterface_**, %struct.JNINativeInterface_*** %4, align 8
  %12 = load %struct.JNINativeInterface_*, %struct.JNINativeInterface_** %11, align 8
  %13 = getelementptr inbounds %struct.JNINativeInterface_, %struct.JNINativeInterface_* %12, i32 0, i32 6
  %14 = load %struct._jobject* (%struct.JNINativeInterface_**, i8*)*, %struct._jobject* (%struct.JNINativeInterface_**, i8*)** %13, align 8
  %15 = load %struct.JNINativeInterface_**, %struct.JNINativeInterface_*** %4, align 8
  %16 = call %struct._jobject* %14(%struct.JNINativeInterface_** noundef %15, i8* noundef getelementptr inbounds ([89 x i8], [89 x i8]* @.str.3, i64 0, i64 0))
  store %struct._jobject* %16, %struct._jobject** %7, align 8
  %17 = load %struct.JNINativeInterface_**, %struct.JNINativeInterface_*** %4, align 8
  %18 = load %struct.JNINativeInterface_*, %struct.JNINativeInterface_** %17, align 8
  %19 = getelementptr inbounds %struct.JNINativeInterface_, %struct.JNINativeInterface_* %18, i32 0, i32 33
  %20 = load %struct._jmethodID* (%struct.JNINativeInterface_**, %struct._jobject*, i8*, i8*)*, %struct._jmethodID* (%struct.JNINativeInterface_**, %struct._jobject*, i8*, i8*)** %19, align 8
  %21 = load %struct.JNINativeInterface_**, %struct.JNINativeInterface_*** %4, align 8
  %22 = load %struct._jobject*, %struct._jobject** %7, align 8
  %23 = call %struct._jmethodID* %20(%struct.JNINativeInterface_** noundef %21, %struct._jobject* noundef %22, i8* noundef getelementptr inbounds ([7 x i8], [7 x i8]* @.str.4, i64 0, i64 0), i8* noundef getelementptr inbounds ([4 x i8], [4 x i8]* @.str.5, i64 0, i64 0))
  store %struct._jmethodID* %23, %struct._jmethodID** %8, align 8
  %24 = load %struct.JNINativeInterface_**, %struct.JNINativeInterface_*** %4, align 8
  %25 = load %struct.JNINativeInterface_*, %struct.JNINativeInterface_** %24, align 8
  %26 = getelementptr inbounds %struct.JNINativeInterface_, %struct.JNINativeInterface_* %25, i32 0, i32 28
  %27 = load %struct._jobject* (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, ...)*, %struct._jobject* (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, ...)** %26, align 8
  %28 = load %struct.JNINativeInterface_**, %struct.JNINativeInterface_*** %4, align 8
  %29 = load %struct._jobject*, %struct._jobject** %7, align 8
  %30 = load %struct._jmethodID*, %struct._jmethodID** %8, align 8
  %31 = call %struct._jobject* (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, ...) %27(%struct.JNINativeInterface_** noundef %28, %struct._jobject* noundef %29, %struct._jmethodID* noundef %30)
  store %struct._jobject* %31, %struct._jobject** %9, align 8
  %32 = load %struct.JNINativeInterface_**, %struct.JNINativeInterface_*** %4, align 8
  %33 = load %struct.JNINativeInterface_*, %struct.JNINativeInterface_** %32, align 8
  %34 = getelementptr inbounds %struct.JNINativeInterface_, %struct.JNINativeInterface_* %33, i32 0, i32 33
  %35 = load %struct._jmethodID* (%struct.JNINativeInterface_**, %struct._jobject*, i8*, i8*)*, %struct._jmethodID* (%struct.JNINativeInterface_**, %struct._jobject*, i8*, i8*)** %34, align 8
  %36 = load %struct.JNINativeInterface_**, %struct.JNINativeInterface_*** %4, align 8
  %37 = load %struct._jobject*, %struct._jobject** %7, align 8
  %38 = call %struct._jmethodID* %35(%struct.JNINativeInterface_** noundef %36, %struct._jobject* noundef %37, i8* noundef getelementptr inbounds ([15 x i8], [15 x i8]* @.str.1, i64 0, i64 0), i8* noundef getelementptr inbounds ([22 x i8], [22 x i8]* @.str.2, i64 0, i64 0))
  store %struct._jmethodID* %38, %struct._jmethodID** %10, align 8
  %39 = load %struct.JNINativeInterface_**, %struct.JNINativeInterface_*** %4, align 8
  %40 = load %struct.JNINativeInterface_*, %struct.JNINativeInterface_** %39, align 8
  %41 = getelementptr inbounds %struct.JNINativeInterface_, %struct.JNINativeInterface_* %40, i32 0, i32 61
  %42 = load void (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, ...)*, void (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, ...)** %41, align 8
  %43 = load %struct.JNINativeInterface_**, %struct.JNINativeInterface_*** %4, align 8
  %44 = load %struct._jobject*, %struct._jobject** %9, align 8
  %45 = load %struct._jmethodID*, %struct._jmethodID** %10, align 8
  %46 = load %struct._jobject*, %struct._jobject** %6, align 8
  call void (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, ...) %42(%struct.JNINativeInterface_** noundef %43, %struct._jobject* noundef %44, %struct._jmethodID* noundef %45, %struct._jobject* noundef %46)
  ret void
}

; Function Attrs: noinline nounwind optnone uwtable
define %struct._jobject* @Java_org_opalj_fpcf_fixtures_xl_llvm_controlflow_bidirectional_CallJavaFunctionFromNativeAndReturn_callMyJavaFunctionFromNativeAndReturn(%struct.JNINativeInterface_** noundef %0, %struct._jobject* noundef %1, %struct._jobject* noundef %2) #0 {
  %4 = alloca %struct.JNINativeInterface_**, align 8
  %5 = alloca %struct._jobject*, align 8
  %6 = alloca %struct._jobject*, align 8
  %7 = alloca %struct._jobject*, align 8
  %8 = alloca %struct._jmethodID*, align 8
  %9 = alloca %struct._jobject*, align 8
  store %struct.JNINativeInterface_** %0, %struct.JNINativeInterface_*** %4, align 8
  store %struct._jobject* %1, %struct._jobject** %5, align 8
  store %struct._jobject* %2, %struct._jobject** %6, align 8
  %10 = load %struct.JNINativeInterface_**, %struct.JNINativeInterface_*** %4, align 8
  %11 = load %struct.JNINativeInterface_*, %struct.JNINativeInterface_** %10, align 8
  %12 = getelementptr inbounds %struct.JNINativeInterface_, %struct.JNINativeInterface_* %11, i32 0, i32 6
  %13 = load %struct._jobject* (%struct.JNINativeInterface_**, i8*)*, %struct._jobject* (%struct.JNINativeInterface_**, i8*)** %12, align 8
  %14 = load %struct.JNINativeInterface_**, %struct.JNINativeInterface_*** %4, align 8
  %15 = call %struct._jobject* %13(%struct.JNINativeInterface_** noundef %14, i8* noundef getelementptr inbounds ([96 x i8], [96 x i8]* @.str.6, i64 0, i64 0))
  store %struct._jobject* %15, %struct._jobject** %7, align 8
  %16 = load %struct.JNINativeInterface_**, %struct.JNINativeInterface_*** %4, align 8
  %17 = load %struct.JNINativeInterface_*, %struct.JNINativeInterface_** %16, align 8
  %18 = getelementptr inbounds %struct.JNINativeInterface_, %struct.JNINativeInterface_* %17, i32 0, i32 33
  %19 = load %struct._jmethodID* (%struct.JNINativeInterface_**, %struct._jobject*, i8*, i8*)*, %struct._jmethodID* (%struct.JNINativeInterface_**, %struct._jobject*, i8*, i8*)** %18, align 8
  %20 = load %struct.JNINativeInterface_**, %struct.JNINativeInterface_*** %4, align 8
  %21 = load %struct._jobject*, %struct._jobject** %7, align 8
  %22 = call %struct._jmethodID* %19(%struct.JNINativeInterface_** noundef %20, %struct._jobject* noundef %21, i8* noundef getelementptr inbounds ([24 x i8], [24 x i8]* @.str.7, i64 0, i64 0), i8* noundef getelementptr inbounds ([39 x i8], [39 x i8]* @.str.8, i64 0, i64 0))
  store %struct._jmethodID* %22, %struct._jmethodID** %8, align 8
  %23 = load %struct.JNINativeInterface_**, %struct.JNINativeInterface_*** %4, align 8
  %24 = load %struct.JNINativeInterface_*, %struct.JNINativeInterface_** %23, align 8
  %25 = getelementptr inbounds %struct.JNINativeInterface_, %struct.JNINativeInterface_* %24, i32 0, i32 34
  %26 = load %struct._jobject* (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, ...)*, %struct._jobject* (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, ...)** %25, align 8
  %27 = load %struct.JNINativeInterface_**, %struct.JNINativeInterface_*** %4, align 8
  %28 = load %struct._jobject*, %struct._jobject** %5, align 8
  %29 = load %struct._jmethodID*, %struct._jmethodID** %8, align 8
  %30 = load %struct._jobject*, %struct._jobject** %6, align 8
  %31 = call %struct._jobject* (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jmethodID*, ...) %26(%struct.JNINativeInterface_** noundef %27, %struct._jobject* noundef %28, %struct._jmethodID* noundef %29, %struct._jobject* noundef %30)
  store %struct._jobject* %31, %struct._jobject** %9, align 8
  %32 = load %struct._jobject*, %struct._jobject** %9, align 8
  ret %struct._jobject* %32
}

; Function Attrs: noinline nounwind optnone uwtable
define void @Java_org_opalj_fpcf_fixtures_xl_llvm_stateaccess_unidirectional_WriteJavaFieldFromNative_setMyfield(%struct.JNINativeInterface_** noundef %0, %struct._jobject* noundef %1, %struct._jobject* noundef %2) #0 {
  %4 = alloca %struct.JNINativeInterface_**, align 8
  %5 = alloca %struct._jobject*, align 8
  %6 = alloca %struct._jobject*, align 8
  %7 = alloca %struct._jobject*, align 8
  %8 = alloca %struct._jfieldID*, align 8
  store %struct.JNINativeInterface_** %0, %struct.JNINativeInterface_*** %4, align 8
  store %struct._jobject* %1, %struct._jobject** %5, align 8
  store %struct._jobject* %2, %struct._jobject** %6, align 8
  %9 = load %struct.JNINativeInterface_**, %struct.JNINativeInterface_*** %4, align 8
  %10 = load %struct.JNINativeInterface_*, %struct.JNINativeInterface_** %9, align 8
  %11 = getelementptr inbounds %struct.JNINativeInterface_, %struct.JNINativeInterface_* %10, i32 0, i32 6
  %12 = load %struct._jobject* (%struct.JNINativeInterface_**, i8*)*, %struct._jobject* (%struct.JNINativeInterface_**, i8*)** %11, align 8
  %13 = load %struct.JNINativeInterface_**, %struct.JNINativeInterface_*** %4, align 8
  %14 = call %struct._jobject* %12(%struct.JNINativeInterface_** noundef %13, i8* noundef getelementptr inbounds ([86 x i8], [86 x i8]* @.str.9, i64 0, i64 0))
  store %struct._jobject* %14, %struct._jobject** %7, align 8
  %15 = load %struct.JNINativeInterface_**, %struct.JNINativeInterface_*** %4, align 8
  %16 = load %struct.JNINativeInterface_*, %struct.JNINativeInterface_** %15, align 8
  %17 = getelementptr inbounds %struct.JNINativeInterface_, %struct.JNINativeInterface_* %16, i32 0, i32 94
  %18 = load %struct._jfieldID* (%struct.JNINativeInterface_**, %struct._jobject*, i8*, i8*)*, %struct._jfieldID* (%struct.JNINativeInterface_**, %struct._jobject*, i8*, i8*)** %17, align 8
  %19 = load %struct.JNINativeInterface_**, %struct.JNINativeInterface_*** %4, align 8
  %20 = load %struct._jobject*, %struct._jobject** %7, align 8
  %21 = call %struct._jfieldID* %18(%struct.JNINativeInterface_** noundef %19, %struct._jobject* noundef %20, i8* noundef getelementptr inbounds ([8 x i8], [8 x i8]* @.str.10, i64 0, i64 0), i8* noundef getelementptr inbounds ([19 x i8], [19 x i8]* @.str.11, i64 0, i64 0))
  store %struct._jfieldID* %21, %struct._jfieldID** %8, align 8
  %22 = load %struct.JNINativeInterface_**, %struct.JNINativeInterface_*** %4, align 8
  %23 = load %struct.JNINativeInterface_*, %struct.JNINativeInterface_** %22, align 8
  %24 = getelementptr inbounds %struct.JNINativeInterface_, %struct.JNINativeInterface_* %23, i32 0, i32 104
  %25 = load void (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*, %struct._jobject*)*, void (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*, %struct._jobject*)** %24, align 8
  %26 = load %struct.JNINativeInterface_**, %struct.JNINativeInterface_*** %4, align 8
  %27 = load %struct._jobject*, %struct._jobject** %5, align 8
  %28 = load %struct._jfieldID*, %struct._jfieldID** %8, align 8
  %29 = load %struct._jobject*, %struct._jobject** %6, align 8
  call void %25(%struct.JNINativeInterface_** noundef %26, %struct._jobject* noundef %27, %struct._jfieldID* noundef %28, %struct._jobject* noundef %29)
  ret void
}

; Function Attrs: noinline nounwind optnone uwtable
define %struct._jobject* @Java_org_opalj_fpcf_fixtures_xl_llvm_stateaccess_unidirectional_ReadJavaFieldFromNative_getMyfield(%struct.JNINativeInterface_** noundef %0, %struct._jobject* noundef %1, %struct._jobject* noundef %2) #0 {
  %4 = alloca %struct.JNINativeInterface_**, align 8
  %5 = alloca %struct._jobject*, align 8
  %6 = alloca %struct._jobject*, align 8
  %7 = alloca %struct._jobject*, align 8
  %8 = alloca %struct._jfieldID*, align 8
  store %struct.JNINativeInterface_** %0, %struct.JNINativeInterface_*** %4, align 8
  store %struct._jobject* %1, %struct._jobject** %5, align 8
  store %struct._jobject* %2, %struct._jobject** %6, align 8
  %9 = load %struct.JNINativeInterface_**, %struct.JNINativeInterface_*** %4, align 8
  %10 = load %struct.JNINativeInterface_*, %struct.JNINativeInterface_** %9, align 8
  %11 = getelementptr inbounds %struct.JNINativeInterface_, %struct.JNINativeInterface_* %10, i32 0, i32 6
  %12 = load %struct._jobject* (%struct.JNINativeInterface_**, i8*)*, %struct._jobject* (%struct.JNINativeInterface_**, i8*)** %11, align 8
  %13 = load %struct.JNINativeInterface_**, %struct.JNINativeInterface_*** %4, align 8
  %14 = call %struct._jobject* %12(%struct.JNINativeInterface_** noundef %13, i8* noundef getelementptr inbounds ([85 x i8], [85 x i8]* @.str.12, i64 0, i64 0))
  store %struct._jobject* %14, %struct._jobject** %7, align 8
  %15 = load %struct.JNINativeInterface_**, %struct.JNINativeInterface_*** %4, align 8
  %16 = load %struct.JNINativeInterface_*, %struct.JNINativeInterface_** %15, align 8
  %17 = getelementptr inbounds %struct.JNINativeInterface_, %struct.JNINativeInterface_* %16, i32 0, i32 94
  %18 = load %struct._jfieldID* (%struct.JNINativeInterface_**, %struct._jobject*, i8*, i8*)*, %struct._jfieldID* (%struct.JNINativeInterface_**, %struct._jobject*, i8*, i8*)** %17, align 8
  %19 = load %struct.JNINativeInterface_**, %struct.JNINativeInterface_*** %4, align 8
  %20 = load %struct._jobject*, %struct._jobject** %7, align 8
  %21 = call %struct._jfieldID* %18(%struct.JNINativeInterface_** noundef %19, %struct._jobject* noundef %20, i8* noundef getelementptr inbounds ([8 x i8], [8 x i8]* @.str.10, i64 0, i64 0), i8* noundef getelementptr inbounds ([19 x i8], [19 x i8]* @.str.11, i64 0, i64 0))
  store %struct._jfieldID* %21, %struct._jfieldID** %8, align 8
  %22 = load %struct.JNINativeInterface_**, %struct.JNINativeInterface_*** %4, align 8
  %23 = load %struct.JNINativeInterface_*, %struct.JNINativeInterface_** %22, align 8
  %24 = getelementptr inbounds %struct.JNINativeInterface_, %struct.JNINativeInterface_* %23, i32 0, i32 95
  %25 = load %struct._jobject* (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*)*, %struct._jobject* (%struct.JNINativeInterface_**, %struct._jobject*, %struct._jfieldID*)** %24, align 8
  %26 = load %struct.JNINativeInterface_**, %struct.JNINativeInterface_*** %4, align 8
  %27 = load %struct._jobject*, %struct._jobject** %5, align 8
  %28 = load %struct._jfieldID*, %struct._jfieldID** %8, align 8
  %29 = call %struct._jobject* %25(%struct.JNINativeInterface_** noundef %26, %struct._jobject* noundef %27, %struct._jfieldID* noundef %28)
  ret %struct._jobject* %29
}

; Function Attrs: noinline nounwind optnone uwtable
define i8* @nativeIdentityFunction(i8* noundef %0) #0 {
  %2 = alloca i8*, align 8
  store i8* %0, i8** %2, align 8
  %3 = load i8*, i8** %2, align 8
  ret i8* %3
}

; Function Attrs: noinline nounwind optnone uwtable
define i8* @returnGlobal() #0 {
  %1 = load i8*, i8** @globb, align 8
  ret i8* %1
}

; Function Attrs: noinline nounwind optnone uwtable
define i8* @returnGlobalCaller() #0 {
  %1 = call i8* @returnGlobal()
  ret i8* %1
}

; Function Attrs: noinline nounwind optnone uwtable
define %struct._jobject* @returnJobjectNullPtr() #0 {
  ret %struct._jobject* null
}

; Function Attrs: noinline nounwind optnone uwtable
define i8* @otherNativeIdCaller() #0 {
  %1 = alloca i8*, align 8
  %2 = alloca i8*, align 8
  %3 = alloca i8*, align 8
  %4 = call i8* @returnGlobal()
  store i8* %4, i8** %1, align 8
  %5 = load i8*, i8** %1, align 8
  %6 = call i8* @nativeIdentityFunction(i8* noundef %5)
  store i8* %6, i8** %2, align 8
  %7 = load i8*, i8** %2, align 8
  %8 = call i8* @nativeIdentityFunction(i8* noundef %7)
  store i8* %8, i8** %3, align 8
  %9 = load i8*, i8** %3, align 8
  ret i8* %9
}

; Function Attrs: noinline nounwind optnone uwtable
define void @setGlobb1() #0 {
  %1 = alloca i8*, align 8
  store i8* getelementptr inbounds ([9 x i8], [9 x i8]* @.str.13, i64 0, i64 0), i8** %1, align 8
  %2 = load i8*, i8** %1, align 8
  store i8* %2, i8** @globb, align 8
  ret void
}

; Function Attrs: noinline nounwind optnone uwtable
define void @setGlobb2() #0 {
  %1 = alloca i8*, align 8
  store i8* getelementptr inbounds ([7 x i8], [7 x i8]* @.str.14, i64 0, i64 0), i8** %1, align 8
  %2 = load i8*, i8** %1, align 8
  store i8* %2, i8** @globb, align 8
  ret void
}

; Function Attrs: noinline nounwind optnone uwtable
define %struct._jobject* @Java_org_opalj_fpcf_fixtures_xl_llvm_controlflow_unidirectional_NativeIdentityFunction_identity(%struct.JNINativeInterface_** noundef %0, %struct._jobject* noundef %1, %struct._jobject* noundef %2) #0 {
  %4 = alloca %struct.JNINativeInterface_**, align 8
  %5 = alloca %struct._jobject*, align 8
  %6 = alloca %struct._jobject*, align 8
  %7 = alloca i8*, align 8
  store %struct.JNINativeInterface_** %0, %struct.JNINativeInterface_*** %4, align 8
  store %struct._jobject* %1, %struct._jobject** %5, align 8
  store %struct._jobject* %2, %struct._jobject** %6, align 8
  %8 = load %struct._jobject*, %struct._jobject** %6, align 8
  %9 = bitcast %struct._jobject* %8 to i8*
  %10 = call i8* @nativeIdentityFunction(i8* noundef %9)
  store i8* %10, i8** %7, align 8
  %11 = load i8*, i8** %7, align 8
  %12 = bitcast i8* %11 to %struct._jobject*
  ret %struct._jobject* %12
}

attributes #0 = { noinline nounwind optnone uwtable "frame-pointer"="all" "min-legal-vector-width"="0" "no-trapping-math"="true" "stack-protector-buffer-size"="8" "target-cpu"="x86-64" "target-features"="+cx8,+fxsr,+mmx,+sse,+sse2,+x87" "tune-cpu"="generic" }

!llvm.module.flags = !{!0, !1, !2, !3}
!llvm.ident = !{!4}

!0 = !{i32 1, !"wchar_size", i32 4}
!1 = !{i32 7, !"PIC Level", i32 2}
!2 = !{i32 7, !"uwtable", i32 1}
!3 = !{i32 7, !"frame-pointer", i32 2}
!4 = !{!"clang version 14.0.0"}
