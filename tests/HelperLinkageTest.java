import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/** Resolve the actual smali call descriptors against compiled Java helpers. */
public final class HelperLinkageTest {
    public static void main(String[] args) throws Exception {
        int count = 0;
        for (String ref : Files.readAllLines(Paths.get(args[0]), StandardCharsets.UTF_8)) {
            if (ref.trim().isEmpty()) continue;
            int arrow = ref.indexOf(";->"), paren = ref.indexOf('(', arrow);
            ClassLoader loader = HelperLinkageTest.class.getClassLoader();
            Class<?> owner = Class.forName(ref.substring(1, arrow).replace('/', '.'), false, loader);
            MethodType type = MethodType.fromMethodDescriptorString(ref.substring(paren), loader);
            Method method = owner.getDeclaredMethod(ref.substring(arrow + 3, paren), type.parameterArray());
            if (!Modifier.isPublic(method.getModifiers()) || !Modifier.isStatic(method.getModifiers()) || method.getReturnType() != type.returnType()) {
                throw new AssertionError("Incompatible helper entry point: " + ref);
            }
            count++;
        }
        if (count == 0) throw new AssertionError("No helper references checked");
        System.out.println("Resolved " + count + " Java helper call descriptors.");
    }
}
