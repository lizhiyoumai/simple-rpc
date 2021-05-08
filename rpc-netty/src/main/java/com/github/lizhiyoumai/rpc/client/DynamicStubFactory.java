/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.lizhiyoumai.rpc.client;

import com.github.lizhiyoumai.rpc.client.stubs.AbstractStub;
import com.github.lizhiyoumai.rpc.client.stubs.RpcRequest;
import com.github.lizhiyoumai.rpc.serialize.SerializeSupport;
import com.github.lizhiyoumai.rpc.transport.Transport;
import com.github.lizhiyoumai.rpc.transport.command.Code;
import com.github.lizhiyoumai.rpc.transport.command.Command;
import com.github.lizhiyoumai.rpc.transport.command.Header;
import com.github.lizhiyoumai.rpc.transport.command.ResponseHeader;
import com.itranswarp.compiler.JavaStringCompiler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * @author huang
 * Date: 2021/05/05
 * 这里使用比较通用的方式来动态生成桩(和语言无关)
 */
public class DynamicStubFactory implements StubFactory{
    private final static String STUB_SOURCE_TEMPLATE =
            "package com.github.lizhiyoumai.rpc.client.stubs;\n" +
            "import com.github.lizhiyoumai.rpc.serialize.SerializeSupport;\n" +
            "\n" +
            "public class %s extends AbstractStub implements %s {\n" +
            "    @Override\n" +
            "    public String %s(String arg) {\n" +
            "        return SerializeSupport.parse(\n" +
            "                invokeRemote(\n" +
            "                        new RpcRequest(\n" +
            "                                \"%s\",\n" +
            "                                \"%s\",\n" +
            "                                SerializeSupport.serialize(arg)\n" +
            "                        )\n" +
            "                )\n" +
            "        );\n" +
            "    }\n" +
            "}";

    @Override
    @SuppressWarnings("unchecked")
    public <T> T createStub(Transport transport, Class<T> serviceClass) {
        try {
            // 填充模板
            String stubSimpleName = serviceClass.getSimpleName() + "Stub";
            String classFullName = serviceClass.getName();
            String stubFullName = "com.github.lizhiyoumai.rpc.client.stubs." + stubSimpleName;
            String methodName = serviceClass.getMethods()[0].getName();

            String source = String.format(STUB_SOURCE_TEMPLATE, stubSimpleName, classFullName, methodName, classFullName, methodName);
            // 编译源代码
            JavaStringCompiler compiler = new JavaStringCompiler();
            Map<String, byte[]> results = compiler.compile(stubSimpleName + ".java", source);
            // 加载编译好的类
            Class<?> clazz = compiler.loadClass(stubFullName, results);

            // 把Transport赋值给桩
            ServiceStub stubInstance = (ServiceStub) clazz.newInstance();
            stubInstance.setTransport(transport);
            // 返回这个桩
            return (T) stubInstance;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }

//        return (T) Proxy.newProxyInstance(serviceClass.getClassLoader(), new Class[]{serviceClass}, new InvocationHandler() {
//            @Override
//            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
//                return SerializeSupport.parse(invokeRemote(transport, new RpcRequest(serviceClass.getName(), method.getName(), SerializeSupport.serialize(args))));
//            }
//        });
    }

//    private byte[] invokeRemote(Transport transport, RpcRequest request) {
//        Header header = new Header(ServiceTypes.TYPE_RPC_REQUEST, 1, RequestIdSupport.next());
//        byte [] payload = SerializeSupport.serialize(request);
//        Command requestCommand = new Command(header, payload);
//        try {
//            Command responseCommand = transport.send(requestCommand).get();
//            ResponseHeader responseHeader = (ResponseHeader) responseCommand.getHeader();
//            if(responseHeader.getCode() == Code.SUCCESS.getCode()) {
//                return responseCommand.getPayload();
//            } else {
//                throw new Exception(responseHeader.getError());
//            }
//
//        } catch (ExecutionException e) {
//            throw new RuntimeException(e.getCause());
//        } catch (Throwable e) {
//            throw new RuntimeException(e);
//        }
//    }
}
