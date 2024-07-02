import os
import re

def find_java_files():
    java_files = []
    for root, _, files in os.walk('.'):
        for file in files:
            if file.endswith('.java'):
                java_files.append(os.path.join(root, file))
    return java_files

def parse_native_functions(java_file):
    with open(java_file, 'r') as file:
        content = file.read()

    package_pattern = r'package\s+([\w\.]+);'
    package_match = re.search(package_pattern, content)
    package = package_match.group(1) if package_match else ''

    class_pattern = r'class\s+(\w+)'
    class_match = re.search(class_pattern, content)
    class_name = class_match.group(1) if class_match else ''

    native_function_pattern = r'(public\s+)?(static\s+)?native\s+(\w+)\s+(\w+)\((.*?)\);'
    native_functions = re.findall(native_function_pattern, content)

    return package, class_name, native_functions

def generate_jni_signature(package, class_name, native_function):
    public, static, return_type, method_name, params = native_function
    params = params.split(',')
    params = [param.strip().split()[-1] for param in params if param.strip()]

    jni_return_type = 'void' if return_type == 'void' else 'jobject'
    jni_params = ['JNIEnv* env'] + (['jclass jThis'] if static else ['jobject jThis'])

    jni_params += ['jobject ' + param for param in params]
    jni_params_str = ', '.join(jni_params)

    jni_method_name = f'Java_{package.replace(".", "_")}_{class_name}_{method_name}'
    class_id = f'{package.replace(".","/")}/{class_name}'
    return f'// {class_id}\nJNIEXPORT {jni_return_type} JNICALL {jni_method_name}({jni_params_str});'

def main():
    java_files = find_java_files()
    for java_file in java_files:
        #print("// " + java_file)
        package, class_name, native_functions = parse_native_functions(java_file)
        for native_function in native_functions:
            jni_signature = generate_jni_signature(package, class_name, native_function)
            print(jni_signature)

if __name__ == "__main__":
    main()

