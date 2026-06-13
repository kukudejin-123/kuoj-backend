package com.kkdj.judge.codeSandbox;

import com.kkdj.judge.codeSandbox.impl.ExampleCodeSandbox;
import com.kkdj.judge.codeSandbox.model.ExecuteCodeRequest;
import com.kkdj.judge.codeSandbox.model.ExecuteCodeResponse;
import com.kkdj.model.enums.QuestionSubmitLanguageEnum;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

@SpringBootTest
class CodeSandboxTest {

    @Value("${codesandbox.type:example}")
    private String type;

    @Test
    void executeCode() {
        CodeSandbox codeSandbox = new ExampleCodeSandbox();
        String code = "int main(){}";
        String language = QuestionSubmitLanguageEnum.JAVA.getValue();
        List<String>inputList = Arrays.asList("1 2","3 4");
        ExecuteCodeRequest excuteCodeRequest = ExecuteCodeRequest.builder()
                .code(code)
                .language(language)
                .inputList(inputList)
                .build();
        ExecuteCodeResponse excuteCodeResponse = codeSandbox.executeCode(excuteCodeRequest);
//        Assertions.assertNotNull(excuteCodeResponse);
    }

    @Test
    void executeCodeByValue() {
        CodeSandbox codeSandbox = CodeSandboxFactory.newInstance(type);
        String code = "int main(){}";
        String language = QuestionSubmitLanguageEnum.JAVA.getValue();
        List<String>inputList = Arrays.asList("1 2","3 4");
        ExecuteCodeRequest excuteCodeRequest = ExecuteCodeRequest.builder()
                .code(code)
                .language(language)
                .inputList(inputList)
                .build();
        ExecuteCodeResponse excuteCodeResponse = codeSandbox.executeCode(excuteCodeRequest);
//        Assertions.assertNotNull(excuteCodeResponse);
    }

    @Test
    void executeCodeByProxy() {
        CodeSandbox codeSandbox = CodeSandboxFactory.newInstance(type);
        codeSandbox = new CodeSandboxProxy(codeSandbox);
        String code = "class Main{\n" +
                "\t\tpublic static void main(String[] args){\n" +
                "\t\t\tint a = Integer.parseInt(args[0]);\n" +
                "\t\t\tint b = Integer.parseInt(args[1]);\n" +
                "\t\t\tSystem.out.println(\"结果：\"+(a+b));\n" +
                "\t}\n" +
                "}";
        String language = QuestionSubmitLanguageEnum.JAVA.getValue();
        List<String>inputList = Arrays.asList("1 2","3 4");
        ExecuteCodeRequest excuteCodeRequest = ExecuteCodeRequest.builder()
                .code(code)
                .language(language)
                .inputList(inputList)
                .build();
        ExecuteCodeResponse excuteCodeResponse = codeSandbox.executeCode(excuteCodeRequest);
//        Assertions.assertNotNull(excuteCodeResponse);
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()){
            String type = scanner.next();
            CodeSandbox codeSandbox = CodeSandboxFactory.newInstance(type);
            String code = "int main(){}";
            String language = QuestionSubmitLanguageEnum.JAVA.getValue();
            List<String>inputList = Arrays.asList("1 2","3 4");
            ExecuteCodeRequest excuteCodeRequest = ExecuteCodeRequest.builder()
                    .code(code)
                    .language(language)
                    .inputList(inputList)
                    .build();
            ExecuteCodeResponse excuteCodeResponse = codeSandbox.executeCode(excuteCodeRequest);
        }
    }

}