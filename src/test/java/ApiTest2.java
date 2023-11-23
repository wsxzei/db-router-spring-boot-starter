import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.junit.Test;

public class ApiTest2 {

    @Test
    public void test_metaObject() {
        Student student = new Student();
        student.setSid("2353545435");
        student.setAge(20);
        student.setName("wzz");

        MetaObject studentMeta = SystemMetaObject.forObject(student);
        boolean hasSetter = studentMeta.hasSetter("name");
        if (hasSetter) {
            studentMeta.setValue("name", "wsx");
        }
        System.out.println(student);
    }
}

class Student extends People{
    private String sid;

    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    @Override
    public String toString() {
        return "Student{" +
                "sid='" + sid + '\'' +
                "} " + super.toString();
    }
}

class People{
    private String name;

    private Integer age;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    @Override
    public String toString() {
        return "People{" +
                "name='" + name + '\'' +
                ", age=" + age +
                '}';
    }
}