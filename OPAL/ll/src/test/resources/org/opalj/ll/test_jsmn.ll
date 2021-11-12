; ModuleID = 'test_jsmn.c'
source_filename = "test_jsmn.c"
target datalayout = "e-m:e-p270:32:32-p271:32:32-p272:64:64-i64:64-f80:128-n8:16:32:64-S128"
target triple = "x86_64-pc-linux-gnu"

%struct.jsmn_parser = type { i32, i32, i32 }
%struct.jsmntok = type { i32, i32, i32, i32 }

@.str = private unnamed_addr constant [38 x i8] c"{\22test\22: \22this is a test\22, \22foo\22: 42}\00", align 1

; Function Attrs: noinline nounwind optnone uwtable
define dso_local i32 @jsmn_parse(%struct.jsmn_parser* %0, i8* %1, i64 %2, %struct.jsmntok* %3, i32 %4) #0 {
  %6 = alloca i32, align 4
  %7 = alloca %struct.jsmn_parser*, align 8
  %8 = alloca i8*, align 8
  %9 = alloca i64, align 8
  %10 = alloca %struct.jsmntok*, align 8
  %11 = alloca i32, align 4
  %12 = alloca i32, align 4
  %13 = alloca i32, align 4
  %14 = alloca %struct.jsmntok*, align 8
  %15 = alloca i32, align 4
  %16 = alloca i8, align 1
  %17 = alloca i32, align 4
  %18 = alloca %struct.jsmntok*, align 8
  store %struct.jsmn_parser* %0, %struct.jsmn_parser** %7, align 8
  store i8* %1, i8** %8, align 8
  store i64 %2, i64* %9, align 8
  store %struct.jsmntok* %3, %struct.jsmntok** %10, align 8
  store i32 %4, i32* %11, align 4
  %19 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %20 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %19, i32 0, i32 1
  %21 = load i32, i32* %20, align 4
  store i32 %21, i32* %15, align 4
  br label %22

22:                                               ; preds = %337, %5
  %23 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %24 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %23, i32 0, i32 0
  %25 = load i32, i32* %24, align 4
  %26 = zext i32 %25 to i64
  %27 = load i64, i64* %9, align 8
  %28 = icmp ult i64 %26, %27
  br i1 %28, label %29, label %39

29:                                               ; preds = %22
  %30 = load i8*, i8** %8, align 8
  %31 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %32 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %31, i32 0, i32 0
  %33 = load i32, i32* %32, align 4
  %34 = zext i32 %33 to i64
  %35 = getelementptr inbounds i8, i8* %30, i64 %34
  %36 = load i8, i8* %35, align 1
  %37 = sext i8 %36 to i32
  %38 = icmp ne i32 %37, 0
  br label %39

39:                                               ; preds = %29, %22
  %40 = phi i1 [ false, %22 ], [ %38, %29 ]
  br i1 %40, label %41, label %342

41:                                               ; preds = %39
  %42 = load i8*, i8** %8, align 8
  %43 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %44 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %43, i32 0, i32 0
  %45 = load i32, i32* %44, align 4
  %46 = zext i32 %45 to i64
  %47 = getelementptr inbounds i8, i8* %42, i64 %46
  %48 = load i8, i8* %47, align 1
  store i8 %48, i8* %16, align 1
  %49 = load i8, i8* %16, align 1
  %50 = sext i8 %49 to i32
  switch i32 %50, label %303 [
    i32 123, label %51
    i32 91, label %51
    i32 125, label %101
    i32 93, label %101
    i32 34, label %183
    i32 9, label %216
    i32 13, label %216
    i32 10, label %216
    i32 32, label %216
    i32 58, label %217
    i32 44, label %224
  ]

51:                                               ; preds = %41, %41
  %52 = load i32, i32* %15, align 4
  %53 = add nsw i32 %52, 1
  store i32 %53, i32* %15, align 4
  %54 = load %struct.jsmntok*, %struct.jsmntok** %10, align 8
  %55 = icmp eq %struct.jsmntok* %54, null
  br i1 %55, label %56, label %57

56:                                               ; preds = %51
  br label %336

57:                                               ; preds = %51
  %58 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %59 = load %struct.jsmntok*, %struct.jsmntok** %10, align 8
  %60 = load i32, i32* %11, align 4
  %61 = zext i32 %60 to i64
  %62 = call %struct.jsmntok* @jsmn_alloc_token(%struct.jsmn_parser* %58, %struct.jsmntok* %59, i64 %61)
  store %struct.jsmntok* %62, %struct.jsmntok** %14, align 8
  %63 = load %struct.jsmntok*, %struct.jsmntok** %14, align 8
  %64 = icmp eq %struct.jsmntok* %63, null
  br i1 %64, label %65, label %66

65:                                               ; preds = %57
  store i32 -1, i32* %6, align 4
  br label %377

66:                                               ; preds = %57
  %67 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %68 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %67, i32 0, i32 2
  %69 = load i32, i32* %68, align 4
  %70 = icmp ne i32 %69, -1
  br i1 %70, label %71, label %82

71:                                               ; preds = %66
  %72 = load %struct.jsmntok*, %struct.jsmntok** %10, align 8
  %73 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %74 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %73, i32 0, i32 2
  %75 = load i32, i32* %74, align 4
  %76 = sext i32 %75 to i64
  %77 = getelementptr inbounds %struct.jsmntok, %struct.jsmntok* %72, i64 %76
  store %struct.jsmntok* %77, %struct.jsmntok** %18, align 8
  %78 = load %struct.jsmntok*, %struct.jsmntok** %18, align 8
  %79 = getelementptr inbounds %struct.jsmntok, %struct.jsmntok* %78, i32 0, i32 3
  %80 = load i32, i32* %79, align 4
  %81 = add nsw i32 %80, 1
  store i32 %81, i32* %79, align 4
  br label %82

82:                                               ; preds = %71, %66
  %83 = load i8, i8* %16, align 1
  %84 = sext i8 %83 to i32
  %85 = icmp eq i32 %84, 123
  %86 = zext i1 %85 to i64
  %87 = select i1 %85, i32 1, i32 2
  %88 = load %struct.jsmntok*, %struct.jsmntok** %14, align 8
  %89 = getelementptr inbounds %struct.jsmntok, %struct.jsmntok* %88, i32 0, i32 0
  store i32 %87, i32* %89, align 4
  %90 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %91 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %90, i32 0, i32 0
  %92 = load i32, i32* %91, align 4
  %93 = load %struct.jsmntok*, %struct.jsmntok** %14, align 8
  %94 = getelementptr inbounds %struct.jsmntok, %struct.jsmntok* %93, i32 0, i32 1
  store i32 %92, i32* %94, align 4
  %95 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %96 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %95, i32 0, i32 1
  %97 = load i32, i32* %96, align 4
  %98 = sub i32 %97, 1
  %99 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %100 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %99, i32 0, i32 2
  store i32 %98, i32* %100, align 4
  br label %336

101:                                              ; preds = %41, %41
  %102 = load %struct.jsmntok*, %struct.jsmntok** %10, align 8
  %103 = icmp eq %struct.jsmntok* %102, null
  br i1 %103, label %104, label %105

104:                                              ; preds = %101
  br label %336

105:                                              ; preds = %101
  %106 = load i8, i8* %16, align 1
  %107 = sext i8 %106 to i32
  %108 = icmp eq i32 %107, 125
  %109 = zext i1 %108 to i64
  %110 = select i1 %108, i32 1, i32 2
  store i32 %110, i32* %17, align 4
  %111 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %112 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %111, i32 0, i32 1
  %113 = load i32, i32* %112, align 4
  %114 = sub i32 %113, 1
  store i32 %114, i32* %13, align 4
  br label %115

115:                                              ; preds = %149, %105
  %116 = load i32, i32* %13, align 4
  %117 = icmp sge i32 %116, 0
  br i1 %117, label %118, label %152

118:                                              ; preds = %115
  %119 = load %struct.jsmntok*, %struct.jsmntok** %10, align 8
  %120 = load i32, i32* %13, align 4
  %121 = sext i32 %120 to i64
  %122 = getelementptr inbounds %struct.jsmntok, %struct.jsmntok* %119, i64 %121
  store %struct.jsmntok* %122, %struct.jsmntok** %14, align 8
  %123 = load %struct.jsmntok*, %struct.jsmntok** %14, align 8
  %124 = getelementptr inbounds %struct.jsmntok, %struct.jsmntok* %123, i32 0, i32 1
  %125 = load i32, i32* %124, align 4
  %126 = icmp ne i32 %125, -1
  br i1 %126, label %127, label %148

127:                                              ; preds = %118
  %128 = load %struct.jsmntok*, %struct.jsmntok** %14, align 8
  %129 = getelementptr inbounds %struct.jsmntok, %struct.jsmntok* %128, i32 0, i32 2
  %130 = load i32, i32* %129, align 4
  %131 = icmp eq i32 %130, -1
  br i1 %131, label %132, label %148

132:                                              ; preds = %127
  %133 = load %struct.jsmntok*, %struct.jsmntok** %14, align 8
  %134 = getelementptr inbounds %struct.jsmntok, %struct.jsmntok* %133, i32 0, i32 0
  %135 = load i32, i32* %134, align 4
  %136 = load i32, i32* %17, align 4
  %137 = icmp ne i32 %135, %136
  br i1 %137, label %138, label %139

138:                                              ; preds = %132
  store i32 -2, i32* %6, align 4
  br label %377

139:                                              ; preds = %132
  %140 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %141 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %140, i32 0, i32 2
  store i32 -1, i32* %141, align 4
  %142 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %143 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %142, i32 0, i32 0
  %144 = load i32, i32* %143, align 4
  %145 = add i32 %144, 1
  %146 = load %struct.jsmntok*, %struct.jsmntok** %14, align 8
  %147 = getelementptr inbounds %struct.jsmntok, %struct.jsmntok* %146, i32 0, i32 2
  store i32 %145, i32* %147, align 4
  br label %152

148:                                              ; preds = %127, %118
  br label %149

149:                                              ; preds = %148
  %150 = load i32, i32* %13, align 4
  %151 = add nsw i32 %150, -1
  store i32 %151, i32* %13, align 4
  br label %115, !llvm.loop !4

152:                                              ; preds = %139, %115
  %153 = load i32, i32* %13, align 4
  %154 = icmp eq i32 %153, -1
  br i1 %154, label %155, label %156

155:                                              ; preds = %152
  store i32 -2, i32* %6, align 4
  br label %377

156:                                              ; preds = %152
  br label %157

157:                                              ; preds = %179, %156
  %158 = load i32, i32* %13, align 4
  %159 = icmp sge i32 %158, 0
  br i1 %159, label %160, label %182

160:                                              ; preds = %157
  %161 = load %struct.jsmntok*, %struct.jsmntok** %10, align 8
  %162 = load i32, i32* %13, align 4
  %163 = sext i32 %162 to i64
  %164 = getelementptr inbounds %struct.jsmntok, %struct.jsmntok* %161, i64 %163
  store %struct.jsmntok* %164, %struct.jsmntok** %14, align 8
  %165 = load %struct.jsmntok*, %struct.jsmntok** %14, align 8
  %166 = getelementptr inbounds %struct.jsmntok, %struct.jsmntok* %165, i32 0, i32 1
  %167 = load i32, i32* %166, align 4
  %168 = icmp ne i32 %167, -1
  br i1 %168, label %169, label %178

169:                                              ; preds = %160
  %170 = load %struct.jsmntok*, %struct.jsmntok** %14, align 8
  %171 = getelementptr inbounds %struct.jsmntok, %struct.jsmntok* %170, i32 0, i32 2
  %172 = load i32, i32* %171, align 4
  %173 = icmp eq i32 %172, -1
  br i1 %173, label %174, label %178

174:                                              ; preds = %169
  %175 = load i32, i32* %13, align 4
  %176 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %177 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %176, i32 0, i32 2
  store i32 %175, i32* %177, align 4
  br label %182

178:                                              ; preds = %169, %160
  br label %179

179:                                              ; preds = %178
  %180 = load i32, i32* %13, align 4
  %181 = add nsw i32 %180, -1
  store i32 %181, i32* %13, align 4
  br label %157, !llvm.loop !6

182:                                              ; preds = %174, %157
  br label %336

183:                                              ; preds = %41
  %184 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %185 = load i8*, i8** %8, align 8
  %186 = load i64, i64* %9, align 8
  %187 = load %struct.jsmntok*, %struct.jsmntok** %10, align 8
  %188 = load i32, i32* %11, align 4
  %189 = zext i32 %188 to i64
  %190 = call i32 @jsmn_parse_string(%struct.jsmn_parser* %184, i8* %185, i64 %186, %struct.jsmntok* %187, i64 %189)
  store i32 %190, i32* %12, align 4
  %191 = load i32, i32* %12, align 4
  %192 = icmp slt i32 %191, 0
  br i1 %192, label %193, label %195

193:                                              ; preds = %183
  %194 = load i32, i32* %12, align 4
  store i32 %194, i32* %6, align 4
  br label %377

195:                                              ; preds = %183
  %196 = load i32, i32* %15, align 4
  %197 = add nsw i32 %196, 1
  store i32 %197, i32* %15, align 4
  %198 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %199 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %198, i32 0, i32 2
  %200 = load i32, i32* %199, align 4
  %201 = icmp ne i32 %200, -1
  br i1 %201, label %202, label %215

202:                                              ; preds = %195
  %203 = load %struct.jsmntok*, %struct.jsmntok** %10, align 8
  %204 = icmp ne %struct.jsmntok* %203, null
  br i1 %204, label %205, label %215

205:                                              ; preds = %202
  %206 = load %struct.jsmntok*, %struct.jsmntok** %10, align 8
  %207 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %208 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %207, i32 0, i32 2
  %209 = load i32, i32* %208, align 4
  %210 = sext i32 %209 to i64
  %211 = getelementptr inbounds %struct.jsmntok, %struct.jsmntok* %206, i64 %210
  %212 = getelementptr inbounds %struct.jsmntok, %struct.jsmntok* %211, i32 0, i32 3
  %213 = load i32, i32* %212, align 4
  %214 = add nsw i32 %213, 1
  store i32 %214, i32* %212, align 4
  br label %215

215:                                              ; preds = %205, %202, %195
  br label %336

216:                                              ; preds = %41, %41, %41, %41
  br label %336

217:                                              ; preds = %41
  %218 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %219 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %218, i32 0, i32 1
  %220 = load i32, i32* %219, align 4
  %221 = sub i32 %220, 1
  %222 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %223 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %222, i32 0, i32 2
  store i32 %221, i32* %223, align 4
  br label %336

224:                                              ; preds = %41
  %225 = load %struct.jsmntok*, %struct.jsmntok** %10, align 8
  %226 = icmp ne %struct.jsmntok* %225, null
  br i1 %226, label %227, label %302

227:                                              ; preds = %224
  %228 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %229 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %228, i32 0, i32 2
  %230 = load i32, i32* %229, align 4
  %231 = icmp ne i32 %230, -1
  br i1 %231, label %232, label %302

232:                                              ; preds = %227
  %233 = load %struct.jsmntok*, %struct.jsmntok** %10, align 8
  %234 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %235 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %234, i32 0, i32 2
  %236 = load i32, i32* %235, align 4
  %237 = sext i32 %236 to i64
  %238 = getelementptr inbounds %struct.jsmntok, %struct.jsmntok* %233, i64 %237
  %239 = getelementptr inbounds %struct.jsmntok, %struct.jsmntok* %238, i32 0, i32 0
  %240 = load i32, i32* %239, align 4
  %241 = icmp ne i32 %240, 2
  br i1 %241, label %242, label %302

242:                                              ; preds = %232
  %243 = load %struct.jsmntok*, %struct.jsmntok** %10, align 8
  %244 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %245 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %244, i32 0, i32 2
  %246 = load i32, i32* %245, align 4
  %247 = sext i32 %246 to i64
  %248 = getelementptr inbounds %struct.jsmntok, %struct.jsmntok* %243, i64 %247
  %249 = getelementptr inbounds %struct.jsmntok, %struct.jsmntok* %248, i32 0, i32 0
  %250 = load i32, i32* %249, align 4
  %251 = icmp ne i32 %250, 1
  br i1 %251, label %252, label %302

252:                                              ; preds = %242
  %253 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %254 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %253, i32 0, i32 1
  %255 = load i32, i32* %254, align 4
  %256 = sub i32 %255, 1
  store i32 %256, i32* %13, align 4
  br label %257

257:                                              ; preds = %298, %252
  %258 = load i32, i32* %13, align 4
  %259 = icmp sge i32 %258, 0
  br i1 %259, label %260, label %301

260:                                              ; preds = %257
  %261 = load %struct.jsmntok*, %struct.jsmntok** %10, align 8
  %262 = load i32, i32* %13, align 4
  %263 = sext i32 %262 to i64
  %264 = getelementptr inbounds %struct.jsmntok, %struct.jsmntok* %261, i64 %263
  %265 = getelementptr inbounds %struct.jsmntok, %struct.jsmntok* %264, i32 0, i32 0
  %266 = load i32, i32* %265, align 4
  %267 = icmp eq i32 %266, 2
  br i1 %267, label %276, label %268

268:                                              ; preds = %260
  %269 = load %struct.jsmntok*, %struct.jsmntok** %10, align 8
  %270 = load i32, i32* %13, align 4
  %271 = sext i32 %270 to i64
  %272 = getelementptr inbounds %struct.jsmntok, %struct.jsmntok* %269, i64 %271
  %273 = getelementptr inbounds %struct.jsmntok, %struct.jsmntok* %272, i32 0, i32 0
  %274 = load i32, i32* %273, align 4
  %275 = icmp eq i32 %274, 1
  br i1 %275, label %276, label %297

276:                                              ; preds = %268, %260
  %277 = load %struct.jsmntok*, %struct.jsmntok** %10, align 8
  %278 = load i32, i32* %13, align 4
  %279 = sext i32 %278 to i64
  %280 = getelementptr inbounds %struct.jsmntok, %struct.jsmntok* %277, i64 %279
  %281 = getelementptr inbounds %struct.jsmntok, %struct.jsmntok* %280, i32 0, i32 1
  %282 = load i32, i32* %281, align 4
  %283 = icmp ne i32 %282, -1
  br i1 %283, label %284, label %296

284:                                              ; preds = %276
  %285 = load %struct.jsmntok*, %struct.jsmntok** %10, align 8
  %286 = load i32, i32* %13, align 4
  %287 = sext i32 %286 to i64
  %288 = getelementptr inbounds %struct.jsmntok, %struct.jsmntok* %285, i64 %287
  %289 = getelementptr inbounds %struct.jsmntok, %struct.jsmntok* %288, i32 0, i32 2
  %290 = load i32, i32* %289, align 4
  %291 = icmp eq i32 %290, -1
  br i1 %291, label %292, label %296

292:                                              ; preds = %284
  %293 = load i32, i32* %13, align 4
  %294 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %295 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %294, i32 0, i32 2
  store i32 %293, i32* %295, align 4
  br label %301

296:                                              ; preds = %284, %276
  br label %297

297:                                              ; preds = %296, %268
  br label %298

298:                                              ; preds = %297
  %299 = load i32, i32* %13, align 4
  %300 = add nsw i32 %299, -1
  store i32 %300, i32* %13, align 4
  br label %257, !llvm.loop !7

301:                                              ; preds = %292, %257
  br label %302

302:                                              ; preds = %301, %242, %232, %227, %224
  br label %336

303:                                              ; preds = %41
  %304 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %305 = load i8*, i8** %8, align 8
  %306 = load i64, i64* %9, align 8
  %307 = load %struct.jsmntok*, %struct.jsmntok** %10, align 8
  %308 = load i32, i32* %11, align 4
  %309 = zext i32 %308 to i64
  %310 = call i32 @jsmn_parse_primitive(%struct.jsmn_parser* %304, i8* %305, i64 %306, %struct.jsmntok* %307, i64 %309)
  store i32 %310, i32* %12, align 4
  %311 = load i32, i32* %12, align 4
  %312 = icmp slt i32 %311, 0
  br i1 %312, label %313, label %315

313:                                              ; preds = %303
  %314 = load i32, i32* %12, align 4
  store i32 %314, i32* %6, align 4
  br label %377

315:                                              ; preds = %303
  %316 = load i32, i32* %15, align 4
  %317 = add nsw i32 %316, 1
  store i32 %317, i32* %15, align 4
  %318 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %319 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %318, i32 0, i32 2
  %320 = load i32, i32* %319, align 4
  %321 = icmp ne i32 %320, -1
  br i1 %321, label %322, label %335

322:                                              ; preds = %315
  %323 = load %struct.jsmntok*, %struct.jsmntok** %10, align 8
  %324 = icmp ne %struct.jsmntok* %323, null
  br i1 %324, label %325, label %335

325:                                              ; preds = %322
  %326 = load %struct.jsmntok*, %struct.jsmntok** %10, align 8
  %327 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %328 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %327, i32 0, i32 2
  %329 = load i32, i32* %328, align 4
  %330 = sext i32 %329 to i64
  %331 = getelementptr inbounds %struct.jsmntok, %struct.jsmntok* %326, i64 %330
  %332 = getelementptr inbounds %struct.jsmntok, %struct.jsmntok* %331, i32 0, i32 3
  %333 = load i32, i32* %332, align 4
  %334 = add nsw i32 %333, 1
  store i32 %334, i32* %332, align 4
  br label %335

335:                                              ; preds = %325, %322, %315
  br label %336

336:                                              ; preds = %335, %302, %217, %216, %215, %182, %104, %82, %56
  br label %337

337:                                              ; preds = %336
  %338 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %339 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %338, i32 0, i32 0
  %340 = load i32, i32* %339, align 4
  %341 = add i32 %340, 1
  store i32 %341, i32* %339, align 4
  br label %22, !llvm.loop !8

342:                                              ; preds = %39
  %343 = load %struct.jsmntok*, %struct.jsmntok** %10, align 8
  %344 = icmp ne %struct.jsmntok* %343, null
  br i1 %344, label %345, label %375

345:                                              ; preds = %342
  %346 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %347 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %346, i32 0, i32 1
  %348 = load i32, i32* %347, align 4
  %349 = sub i32 %348, 1
  store i32 %349, i32* %13, align 4
  br label %350

350:                                              ; preds = %371, %345
  %351 = load i32, i32* %13, align 4
  %352 = icmp sge i32 %351, 0
  br i1 %352, label %353, label %374

353:                                              ; preds = %350
  %354 = load %struct.jsmntok*, %struct.jsmntok** %10, align 8
  %355 = load i32, i32* %13, align 4
  %356 = sext i32 %355 to i64
  %357 = getelementptr inbounds %struct.jsmntok, %struct.jsmntok* %354, i64 %356
  %358 = getelementptr inbounds %struct.jsmntok, %struct.jsmntok* %357, i32 0, i32 1
  %359 = load i32, i32* %358, align 4
  %360 = icmp ne i32 %359, -1
  br i1 %360, label %361, label %370

361:                                              ; preds = %353
  %362 = load %struct.jsmntok*, %struct.jsmntok** %10, align 8
  %363 = load i32, i32* %13, align 4
  %364 = sext i32 %363 to i64
  %365 = getelementptr inbounds %struct.jsmntok, %struct.jsmntok* %362, i64 %364
  %366 = getelementptr inbounds %struct.jsmntok, %struct.jsmntok* %365, i32 0, i32 2
  %367 = load i32, i32* %366, align 4
  %368 = icmp eq i32 %367, -1
  br i1 %368, label %369, label %370

369:                                              ; preds = %361
  store i32 -3, i32* %6, align 4
  br label %377

370:                                              ; preds = %361, %353
  br label %371

371:                                              ; preds = %370
  %372 = load i32, i32* %13, align 4
  %373 = add nsw i32 %372, -1
  store i32 %373, i32* %13, align 4
  br label %350, !llvm.loop !9

374:                                              ; preds = %350
  br label %375

375:                                              ; preds = %374, %342
  %376 = load i32, i32* %15, align 4
  store i32 %376, i32* %6, align 4
  br label %377

377:                                              ; preds = %375, %369, %313, %193, %155, %138, %65
  %378 = load i32, i32* %6, align 4
  ret i32 %378
}

; Function Attrs: noinline nounwind optnone uwtable
define internal %struct.jsmntok* @jsmn_alloc_token(%struct.jsmn_parser* %0, %struct.jsmntok* %1, i64 %2) #0 {
  %4 = alloca %struct.jsmntok*, align 8
  %5 = alloca %struct.jsmn_parser*, align 8
  %6 = alloca %struct.jsmntok*, align 8
  %7 = alloca i64, align 8
  %8 = alloca %struct.jsmntok*, align 8
  store %struct.jsmn_parser* %0, %struct.jsmn_parser** %5, align 8
  store %struct.jsmntok* %1, %struct.jsmntok** %6, align 8
  store i64 %2, i64* %7, align 8
  %9 = load %struct.jsmn_parser*, %struct.jsmn_parser** %5, align 8
  %10 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %9, i32 0, i32 1
  %11 = load i32, i32* %10, align 4
  %12 = zext i32 %11 to i64
  %13 = load i64, i64* %7, align 8
  %14 = icmp uge i64 %12, %13
  br i1 %14, label %15, label %16

15:                                               ; preds = %3
  store %struct.jsmntok* null, %struct.jsmntok** %4, align 8
  br label %31

16:                                               ; preds = %3
  %17 = load %struct.jsmntok*, %struct.jsmntok** %6, align 8
  %18 = load %struct.jsmn_parser*, %struct.jsmn_parser** %5, align 8
  %19 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %18, i32 0, i32 1
  %20 = load i32, i32* %19, align 4
  %21 = add i32 %20, 1
  store i32 %21, i32* %19, align 4
  %22 = zext i32 %20 to i64
  %23 = getelementptr inbounds %struct.jsmntok, %struct.jsmntok* %17, i64 %22
  store %struct.jsmntok* %23, %struct.jsmntok** %8, align 8
  %24 = load %struct.jsmntok*, %struct.jsmntok** %8, align 8
  %25 = getelementptr inbounds %struct.jsmntok, %struct.jsmntok* %24, i32 0, i32 2
  store i32 -1, i32* %25, align 4
  %26 = load %struct.jsmntok*, %struct.jsmntok** %8, align 8
  %27 = getelementptr inbounds %struct.jsmntok, %struct.jsmntok* %26, i32 0, i32 1
  store i32 -1, i32* %27, align 4
  %28 = load %struct.jsmntok*, %struct.jsmntok** %8, align 8
  %29 = getelementptr inbounds %struct.jsmntok, %struct.jsmntok* %28, i32 0, i32 3
  store i32 0, i32* %29, align 4
  %30 = load %struct.jsmntok*, %struct.jsmntok** %8, align 8
  store %struct.jsmntok* %30, %struct.jsmntok** %4, align 8
  br label %31

31:                                               ; preds = %16, %15
  %32 = load %struct.jsmntok*, %struct.jsmntok** %4, align 8
  ret %struct.jsmntok* %32
}

; Function Attrs: noinline nounwind optnone uwtable
define internal i32 @jsmn_parse_string(%struct.jsmn_parser* %0, i8* %1, i64 %2, %struct.jsmntok* %3, i64 %4) #0 {
  %6 = alloca i32, align 4
  %7 = alloca %struct.jsmn_parser*, align 8
  %8 = alloca i8*, align 8
  %9 = alloca i64, align 8
  %10 = alloca %struct.jsmntok*, align 8
  %11 = alloca i64, align 8
  %12 = alloca %struct.jsmntok*, align 8
  %13 = alloca i32, align 4
  %14 = alloca i8, align 1
  %15 = alloca i32, align 4
  store %struct.jsmn_parser* %0, %struct.jsmn_parser** %7, align 8
  store i8* %1, i8** %8, align 8
  store i64 %2, i64* %9, align 8
  store %struct.jsmntok* %3, %struct.jsmntok** %10, align 8
  store i64 %4, i64* %11, align 8
  %16 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %17 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %16, i32 0, i32 0
  %18 = load i32, i32* %17, align 4
  store i32 %18, i32* %13, align 4
  %19 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %20 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %19, i32 0, i32 0
  %21 = load i32, i32* %20, align 4
  %22 = add i32 %21, 1
  store i32 %22, i32* %20, align 4
  br label %23

23:                                               ; preds = %211, %5
  %24 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %25 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %24, i32 0, i32 0
  %26 = load i32, i32* %25, align 4
  %27 = zext i32 %26 to i64
  %28 = load i64, i64* %9, align 8
  %29 = icmp ult i64 %27, %28
  br i1 %29, label %30, label %40

30:                                               ; preds = %23
  %31 = load i8*, i8** %8, align 8
  %32 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %33 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %32, i32 0, i32 0
  %34 = load i32, i32* %33, align 4
  %35 = zext i32 %34 to i64
  %36 = getelementptr inbounds i8, i8* %31, i64 %35
  %37 = load i8, i8* %36, align 1
  %38 = sext i8 %37 to i32
  %39 = icmp ne i32 %38, 0
  br label %40

40:                                               ; preds = %30, %23
  %41 = phi i1 [ false, %23 ], [ %39, %30 ]
  br i1 %41, label %42, label %216

42:                                               ; preds = %40
  %43 = load i8*, i8** %8, align 8
  %44 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %45 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %44, i32 0, i32 0
  %46 = load i32, i32* %45, align 4
  %47 = zext i32 %46 to i64
  %48 = getelementptr inbounds i8, i8* %43, i64 %47
  %49 = load i8, i8* %48, align 1
  store i8 %49, i8* %14, align 1
  %50 = load i8, i8* %14, align 1
  %51 = sext i8 %50 to i32
  %52 = icmp eq i32 %51, 34
  br i1 %52, label %53, label %75

53:                                               ; preds = %42
  %54 = load %struct.jsmntok*, %struct.jsmntok** %10, align 8
  %55 = icmp eq %struct.jsmntok* %54, null
  br i1 %55, label %56, label %57

56:                                               ; preds = %53
  store i32 0, i32* %6, align 4
  br label %220

57:                                               ; preds = %53
  %58 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %59 = load %struct.jsmntok*, %struct.jsmntok** %10, align 8
  %60 = load i64, i64* %11, align 8
  %61 = call %struct.jsmntok* @jsmn_alloc_token(%struct.jsmn_parser* %58, %struct.jsmntok* %59, i64 %60)
  store %struct.jsmntok* %61, %struct.jsmntok** %12, align 8
  %62 = load %struct.jsmntok*, %struct.jsmntok** %12, align 8
  %63 = icmp eq %struct.jsmntok* %62, null
  br i1 %63, label %64, label %68

64:                                               ; preds = %57
  %65 = load i32, i32* %13, align 4
  %66 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %67 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %66, i32 0, i32 0
  store i32 %65, i32* %67, align 4
  store i32 -1, i32* %6, align 4
  br label %220

68:                                               ; preds = %57
  %69 = load %struct.jsmntok*, %struct.jsmntok** %12, align 8
  %70 = load i32, i32* %13, align 4
  %71 = add nsw i32 %70, 1
  %72 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %73 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %72, i32 0, i32 0
  %74 = load i32, i32* %73, align 4
  call void @jsmn_fill_token(%struct.jsmntok* %69, i32 4, i32 %71, i32 %74)
  store i32 0, i32* %6, align 4
  br label %220

75:                                               ; preds = %42
  %76 = load i8, i8* %14, align 1
  %77 = sext i8 %76 to i32
  %78 = icmp eq i32 %77, 92
  br i1 %78, label %79, label %210

79:                                               ; preds = %75
  %80 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %81 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %80, i32 0, i32 0
  %82 = load i32, i32* %81, align 4
  %83 = add i32 %82, 1
  %84 = zext i32 %83 to i64
  %85 = load i64, i64* %9, align 8
  %86 = icmp ult i64 %84, %85
  br i1 %86, label %87, label %210

87:                                               ; preds = %79
  %88 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %89 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %88, i32 0, i32 0
  %90 = load i32, i32* %89, align 4
  %91 = add i32 %90, 1
  store i32 %91, i32* %89, align 4
  %92 = load i8*, i8** %8, align 8
  %93 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %94 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %93, i32 0, i32 0
  %95 = load i32, i32* %94, align 4
  %96 = zext i32 %95 to i64
  %97 = getelementptr inbounds i8, i8* %92, i64 %96
  %98 = load i8, i8* %97, align 1
  %99 = sext i8 %98 to i32
  switch i32 %99, label %205 [
    i32 34, label %100
    i32 47, label %100
    i32 92, label %100
    i32 98, label %100
    i32 102, label %100
    i32 114, label %100
    i32 110, label %100
    i32 116, label %100
    i32 117, label %101
  ]

100:                                              ; preds = %87, %87, %87, %87, %87, %87, %87, %87
  br label %209

101:                                              ; preds = %87
  %102 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %103 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %102, i32 0, i32 0
  %104 = load i32, i32* %103, align 4
  %105 = add i32 %104, 1
  store i32 %105, i32* %103, align 4
  store i32 0, i32* %15, align 4
  br label %106

106:                                              ; preds = %197, %101
  %107 = load i32, i32* %15, align 4
  %108 = icmp slt i32 %107, 4
  br i1 %108, label %109, label %126

109:                                              ; preds = %106
  %110 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %111 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %110, i32 0, i32 0
  %112 = load i32, i32* %111, align 4
  %113 = zext i32 %112 to i64
  %114 = load i64, i64* %9, align 8
  %115 = icmp ult i64 %113, %114
  br i1 %115, label %116, label %126

116:                                              ; preds = %109
  %117 = load i8*, i8** %8, align 8
  %118 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %119 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %118, i32 0, i32 0
  %120 = load i32, i32* %119, align 4
  %121 = zext i32 %120 to i64
  %122 = getelementptr inbounds i8, i8* %117, i64 %121
  %123 = load i8, i8* %122, align 1
  %124 = sext i8 %123 to i32
  %125 = icmp ne i32 %124, 0
  br label %126

126:                                              ; preds = %116, %109, %106
  %127 = phi i1 [ false, %109 ], [ false, %106 ], [ %125, %116 ]
  br i1 %127, label %128, label %200

128:                                              ; preds = %126
  %129 = load i8*, i8** %8, align 8
  %130 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %131 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %130, i32 0, i32 0
  %132 = load i32, i32* %131, align 4
  %133 = zext i32 %132 to i64
  %134 = getelementptr inbounds i8, i8* %129, i64 %133
  %135 = load i8, i8* %134, align 1
  %136 = sext i8 %135 to i32
  %137 = icmp sge i32 %136, 48
  br i1 %137, label %138, label %148

138:                                              ; preds = %128
  %139 = load i8*, i8** %8, align 8
  %140 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %141 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %140, i32 0, i32 0
  %142 = load i32, i32* %141, align 4
  %143 = zext i32 %142 to i64
  %144 = getelementptr inbounds i8, i8* %139, i64 %143
  %145 = load i8, i8* %144, align 1
  %146 = sext i8 %145 to i32
  %147 = icmp sle i32 %146, 57
  br i1 %147, label %192, label %148

148:                                              ; preds = %138, %128
  %149 = load i8*, i8** %8, align 8
  %150 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %151 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %150, i32 0, i32 0
  %152 = load i32, i32* %151, align 4
  %153 = zext i32 %152 to i64
  %154 = getelementptr inbounds i8, i8* %149, i64 %153
  %155 = load i8, i8* %154, align 1
  %156 = sext i8 %155 to i32
  %157 = icmp sge i32 %156, 65
  br i1 %157, label %158, label %168

158:                                              ; preds = %148
  %159 = load i8*, i8** %8, align 8
  %160 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %161 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %160, i32 0, i32 0
  %162 = load i32, i32* %161, align 4
  %163 = zext i32 %162 to i64
  %164 = getelementptr inbounds i8, i8* %159, i64 %163
  %165 = load i8, i8* %164, align 1
  %166 = sext i8 %165 to i32
  %167 = icmp sle i32 %166, 70
  br i1 %167, label %192, label %168

168:                                              ; preds = %158, %148
  %169 = load i8*, i8** %8, align 8
  %170 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %171 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %170, i32 0, i32 0
  %172 = load i32, i32* %171, align 4
  %173 = zext i32 %172 to i64
  %174 = getelementptr inbounds i8, i8* %169, i64 %173
  %175 = load i8, i8* %174, align 1
  %176 = sext i8 %175 to i32
  %177 = icmp sge i32 %176, 97
  br i1 %177, label %178, label %188

178:                                              ; preds = %168
  %179 = load i8*, i8** %8, align 8
  %180 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %181 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %180, i32 0, i32 0
  %182 = load i32, i32* %181, align 4
  %183 = zext i32 %182 to i64
  %184 = getelementptr inbounds i8, i8* %179, i64 %183
  %185 = load i8, i8* %184, align 1
  %186 = sext i8 %185 to i32
  %187 = icmp sle i32 %186, 102
  br i1 %187, label %192, label %188

188:                                              ; preds = %178, %168
  %189 = load i32, i32* %13, align 4
  %190 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %191 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %190, i32 0, i32 0
  store i32 %189, i32* %191, align 4
  store i32 -2, i32* %6, align 4
  br label %220

192:                                              ; preds = %178, %158, %138
  %193 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %194 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %193, i32 0, i32 0
  %195 = load i32, i32* %194, align 4
  %196 = add i32 %195, 1
  store i32 %196, i32* %194, align 4
  br label %197

197:                                              ; preds = %192
  %198 = load i32, i32* %15, align 4
  %199 = add nsw i32 %198, 1
  store i32 %199, i32* %15, align 4
  br label %106, !llvm.loop !10

200:                                              ; preds = %126
  %201 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %202 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %201, i32 0, i32 0
  %203 = load i32, i32* %202, align 4
  %204 = add i32 %203, -1
  store i32 %204, i32* %202, align 4
  br label %209

205:                                              ; preds = %87
  %206 = load i32, i32* %13, align 4
  %207 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %208 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %207, i32 0, i32 0
  store i32 %206, i32* %208, align 4
  store i32 -2, i32* %6, align 4
  br label %220

209:                                              ; preds = %200, %100
  br label %210

210:                                              ; preds = %209, %79, %75
  br label %211

211:                                              ; preds = %210
  %212 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %213 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %212, i32 0, i32 0
  %214 = load i32, i32* %213, align 4
  %215 = add i32 %214, 1
  store i32 %215, i32* %213, align 4
  br label %23, !llvm.loop !11

216:                                              ; preds = %40
  %217 = load i32, i32* %13, align 4
  %218 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %219 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %218, i32 0, i32 0
  store i32 %217, i32* %219, align 4
  store i32 -3, i32* %6, align 4
  br label %220

220:                                              ; preds = %216, %205, %188, %68, %64, %56
  %221 = load i32, i32* %6, align 4
  ret i32 %221
}

; Function Attrs: noinline nounwind optnone uwtable
define internal i32 @jsmn_parse_primitive(%struct.jsmn_parser* %0, i8* %1, i64 %2, %struct.jsmntok* %3, i64 %4) #0 {
  %6 = alloca i32, align 4
  %7 = alloca %struct.jsmn_parser*, align 8
  %8 = alloca i8*, align 8
  %9 = alloca i64, align 8
  %10 = alloca %struct.jsmntok*, align 8
  %11 = alloca i64, align 8
  %12 = alloca %struct.jsmntok*, align 8
  %13 = alloca i32, align 4
  store %struct.jsmn_parser* %0, %struct.jsmn_parser** %7, align 8
  store i8* %1, i8** %8, align 8
  store i64 %2, i64* %9, align 8
  store %struct.jsmntok* %3, %struct.jsmntok** %10, align 8
  store i64 %4, i64* %11, align 8
  %14 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %15 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %14, i32 0, i32 0
  %16 = load i32, i32* %15, align 4
  store i32 %16, i32* %13, align 4
  br label %17

17:                                               ; preds = %72, %5
  %18 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %19 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %18, i32 0, i32 0
  %20 = load i32, i32* %19, align 4
  %21 = zext i32 %20 to i64
  %22 = load i64, i64* %9, align 8
  %23 = icmp ult i64 %21, %22
  br i1 %23, label %24, label %34

24:                                               ; preds = %17
  %25 = load i8*, i8** %8, align 8
  %26 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %27 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %26, i32 0, i32 0
  %28 = load i32, i32* %27, align 4
  %29 = zext i32 %28 to i64
  %30 = getelementptr inbounds i8, i8* %25, i64 %29
  %31 = load i8, i8* %30, align 1
  %32 = sext i8 %31 to i32
  %33 = icmp ne i32 %32, 0
  br label %34

34:                                               ; preds = %24, %17
  %35 = phi i1 [ false, %17 ], [ %33, %24 ]
  br i1 %35, label %36, label %77

36:                                               ; preds = %34
  %37 = load i8*, i8** %8, align 8
  %38 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %39 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %38, i32 0, i32 0
  %40 = load i32, i32* %39, align 4
  %41 = zext i32 %40 to i64
  %42 = getelementptr inbounds i8, i8* %37, i64 %41
  %43 = load i8, i8* %42, align 1
  %44 = sext i8 %43 to i32
  switch i32 %44, label %46 [
    i32 58, label %45
    i32 9, label %45
    i32 13, label %45
    i32 10, label %45
    i32 32, label %45
    i32 44, label %45
    i32 93, label %45
    i32 125, label %45
  ]

45:                                               ; preds = %36, %36, %36, %36, %36, %36, %36, %36
  br label %78

46:                                               ; preds = %36
  br label %47

47:                                               ; preds = %46
  %48 = load i8*, i8** %8, align 8
  %49 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %50 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %49, i32 0, i32 0
  %51 = load i32, i32* %50, align 4
  %52 = zext i32 %51 to i64
  %53 = getelementptr inbounds i8, i8* %48, i64 %52
  %54 = load i8, i8* %53, align 1
  %55 = sext i8 %54 to i32
  %56 = icmp slt i32 %55, 32
  br i1 %56, label %67, label %57

57:                                               ; preds = %47
  %58 = load i8*, i8** %8, align 8
  %59 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %60 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %59, i32 0, i32 0
  %61 = load i32, i32* %60, align 4
  %62 = zext i32 %61 to i64
  %63 = getelementptr inbounds i8, i8* %58, i64 %62
  %64 = load i8, i8* %63, align 1
  %65 = sext i8 %64 to i32
  %66 = icmp sge i32 %65, 127
  br i1 %66, label %67, label %71

67:                                               ; preds = %57, %47
  %68 = load i32, i32* %13, align 4
  %69 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %70 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %69, i32 0, i32 0
  store i32 %68, i32* %70, align 4
  store i32 -2, i32* %6, align 4
  br label %107

71:                                               ; preds = %57
  br label %72

72:                                               ; preds = %71
  %73 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %74 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %73, i32 0, i32 0
  %75 = load i32, i32* %74, align 4
  %76 = add i32 %75, 1
  store i32 %76, i32* %74, align 4
  br label %17, !llvm.loop !12

77:                                               ; preds = %34
  br label %78

78:                                               ; preds = %77, %45
  %79 = load %struct.jsmntok*, %struct.jsmntok** %10, align 8
  %80 = icmp eq %struct.jsmntok* %79, null
  br i1 %80, label %81, label %86

81:                                               ; preds = %78
  %82 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %83 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %82, i32 0, i32 0
  %84 = load i32, i32* %83, align 4
  %85 = add i32 %84, -1
  store i32 %85, i32* %83, align 4
  store i32 0, i32* %6, align 4
  br label %107

86:                                               ; preds = %78
  %87 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %88 = load %struct.jsmntok*, %struct.jsmntok** %10, align 8
  %89 = load i64, i64* %11, align 8
  %90 = call %struct.jsmntok* @jsmn_alloc_token(%struct.jsmn_parser* %87, %struct.jsmntok* %88, i64 %89)
  store %struct.jsmntok* %90, %struct.jsmntok** %12, align 8
  %91 = load %struct.jsmntok*, %struct.jsmntok** %12, align 8
  %92 = icmp eq %struct.jsmntok* %91, null
  br i1 %92, label %93, label %97

93:                                               ; preds = %86
  %94 = load i32, i32* %13, align 4
  %95 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %96 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %95, i32 0, i32 0
  store i32 %94, i32* %96, align 4
  store i32 -1, i32* %6, align 4
  br label %107

97:                                               ; preds = %86
  %98 = load %struct.jsmntok*, %struct.jsmntok** %12, align 8
  %99 = load i32, i32* %13, align 4
  %100 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %101 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %100, i32 0, i32 0
  %102 = load i32, i32* %101, align 4
  call void @jsmn_fill_token(%struct.jsmntok* %98, i32 8, i32 %99, i32 %102)
  %103 = load %struct.jsmn_parser*, %struct.jsmn_parser** %7, align 8
  %104 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %103, i32 0, i32 0
  %105 = load i32, i32* %104, align 4
  %106 = add i32 %105, -1
  store i32 %106, i32* %104, align 4
  store i32 0, i32* %6, align 4
  br label %107

107:                                              ; preds = %97, %93, %81, %67
  %108 = load i32, i32* %6, align 4
  ret i32 %108
}

; Function Attrs: noinline nounwind optnone uwtable
define dso_local void @jsmn_init(%struct.jsmn_parser* %0) #0 {
  %2 = alloca %struct.jsmn_parser*, align 8
  store %struct.jsmn_parser* %0, %struct.jsmn_parser** %2, align 8
  %3 = load %struct.jsmn_parser*, %struct.jsmn_parser** %2, align 8
  %4 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %3, i32 0, i32 0
  store i32 0, i32* %4, align 4
  %5 = load %struct.jsmn_parser*, %struct.jsmn_parser** %2, align 8
  %6 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %5, i32 0, i32 1
  store i32 0, i32* %6, align 4
  %7 = load %struct.jsmn_parser*, %struct.jsmn_parser** %2, align 8
  %8 = getelementptr inbounds %struct.jsmn_parser, %struct.jsmn_parser* %7, i32 0, i32 2
  store i32 -1, i32* %8, align 4
  ret void
}

; Function Attrs: noinline nounwind optnone uwtable
define dso_local i32 @main() #0 {
  %1 = alloca i8*, align 8
  %2 = alloca %struct.jsmn_parser, align 4
  %3 = alloca [128 x %struct.jsmntok], align 16
  %4 = alloca i32, align 4
  store i8* getelementptr inbounds ([38 x i8], [38 x i8]* @.str, i64 0, i64 0), i8** %1, align 8
  call void @jsmn_init(%struct.jsmn_parser* %2)
  %5 = load i8*, i8** %1, align 8
  %6 = load i8*, i8** %1, align 8
  %7 = call i64 @strlen(i8* %6) #2
  %8 = getelementptr inbounds [128 x %struct.jsmntok], [128 x %struct.jsmntok]* %3, i64 0, i64 0
  %9 = call i32 @jsmn_parse(%struct.jsmn_parser* %2, i8* %5, i64 %7, %struct.jsmntok* %8, i32 128)
  store i32 %9, i32* %4, align 4
  ret i32 0
}

; Function Attrs: nounwind readonly willreturn
declare dso_local i64 @strlen(i8*) #1

; Function Attrs: noinline nounwind optnone uwtable
define internal void @jsmn_fill_token(%struct.jsmntok* %0, i32 %1, i32 %2, i32 %3) #0 {
  %5 = alloca %struct.jsmntok*, align 8
  %6 = alloca i32, align 4
  %7 = alloca i32, align 4
  %8 = alloca i32, align 4
  store %struct.jsmntok* %0, %struct.jsmntok** %5, align 8
  store i32 %1, i32* %6, align 4
  store i32 %2, i32* %7, align 4
  store i32 %3, i32* %8, align 4
  %9 = load i32, i32* %6, align 4
  %10 = load %struct.jsmntok*, %struct.jsmntok** %5, align 8
  %11 = getelementptr inbounds %struct.jsmntok, %struct.jsmntok* %10, i32 0, i32 0
  store i32 %9, i32* %11, align 4
  %12 = load i32, i32* %7, align 4
  %13 = load %struct.jsmntok*, %struct.jsmntok** %5, align 8
  %14 = getelementptr inbounds %struct.jsmntok, %struct.jsmntok* %13, i32 0, i32 1
  store i32 %12, i32* %14, align 4
  %15 = load i32, i32* %8, align 4
  %16 = load %struct.jsmntok*, %struct.jsmntok** %5, align 8
  %17 = getelementptr inbounds %struct.jsmntok, %struct.jsmntok* %16, i32 0, i32 2
  store i32 %15, i32* %17, align 4
  %18 = load %struct.jsmntok*, %struct.jsmntok** %5, align 8
  %19 = getelementptr inbounds %struct.jsmntok, %struct.jsmntok* %18, i32 0, i32 3
  store i32 0, i32* %19, align 4
  ret void
}

attributes #0 = { noinline nounwind optnone uwtable "frame-pointer"="all" "min-legal-vector-width"="0" "no-trapping-math"="true" "stack-protector-buffer-size"="8" "target-cpu"="x86-64" "target-features"="+cx8,+fxsr,+mmx,+sse,+sse2,+x87" "tune-cpu"="generic" }
attributes #1 = { nounwind readonly willreturn "frame-pointer"="all" "no-trapping-math"="true" "stack-protector-buffer-size"="8" "target-cpu"="x86-64" "target-features"="+cx8,+fxsr,+mmx,+sse,+sse2,+x87" "tune-cpu"="generic" }
attributes #2 = { nounwind readonly willreturn }

!llvm.module.flags = !{!0, !1, !2}
!llvm.ident = !{!3}

!0 = !{i32 1, !"wchar_size", i32 4}
!1 = !{i32 7, !"uwtable", i32 1}
!2 = !{i32 7, !"frame-pointer", i32 2}
!3 = !{!"Ubuntu clang version 13.0.0-2"}
!4 = distinct !{!4, !5}
!5 = !{!"llvm.loop.mustprogress"}
!6 = distinct !{!6, !5}
!7 = distinct !{!7, !5}
!8 = distinct !{!8, !5}
!9 = distinct !{!9, !5}
!10 = distinct !{!10, !5}
!11 = distinct !{!11, !5}
!12 = distinct !{!12, !5}
