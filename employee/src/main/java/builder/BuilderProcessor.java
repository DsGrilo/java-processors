package builder;

import com.google.auto.service.AutoService;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import java.io.PrintWriter;
import java.util.Set;
import java.util.stream.Collectors;


@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_21)
@SupportedAnnotationTypes(value = "custom.annotations.Builder")
public class BuilderProcessor extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        annotations.forEach(annotation ->
                roundEnv.getElementsAnnotatedWith(annotation)
                        .forEach(this::generatedBuilderFile));
        return true;
    }

    public void generatedBuilderFile(Element element){
        var className = element.getSimpleName().toString();
        var packageName = element.getEnclosingElement().toString();
        var builderName = className + "Builder";
        var builderFullName = packageName + "." + builderName;

         var fields = element.getEnclosedElements()
                 .stream().filter(e -> ElementKind.FIELD.equals(e.getKind())).toList();

         try (PrintWriter writer = new PrintWriter(
                 processingEnv.getFiler().createSourceFile(builderFullName).openWriter())){

             writer.println("""
                        package %s;
                        
                        public class %s {
                     """.formatted(packageName, builderName));

             fields.forEach(field ->
                     writer.print("""
                               private %s %s
                             """.formatted(field.asType().toString(), field.getSimpleName())));

             writer.println();

             fields.forEach(field ->
                     writer.println("""
                             public %s %s(%s value) {
                             %s = value;
                             return this;
                             }
                             """.formatted(builderName, field.getSimpleName(),
                             field.asType().toString(), field.getSimpleName())));

             writer.println("""
                     public %s build(){
                      return new %s(%s)
                      }
                     """.formatted(className, className,
                     fields.stream().map(Element::getSimpleName).collect(Collectors.joining(", "))));
             writer.println("}");

         }catch (Exception e){
             e.printStackTrace();
         }
    }
}
