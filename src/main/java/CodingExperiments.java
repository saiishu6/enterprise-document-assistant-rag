import java.util.List;
import java.util.function.Predicate;

public class CodingExperiments {

     public static void main(String[] args)
    {
        System.out.println("Coding Experiments");
        List<String> employees = List.of(
                "John", "Sarah", "Sam", "Alexander", "Steve"
        );

        List<String> newList = employees.stream().filter(employee -> employee.startsWith("S")).toList();
        System.out.println("New List: " + newList);
    }
}
