package kurento.wisonic.test.test;

public class TestClass extends BaseTest {

    private TestClass2 a = new TestClass2();

    public TestClass() throws Exception {
    }


    public static void main(String[] args) throws Exception {
        System.out.println(test);
    }

    private void print1() {
        print(1);
    }

    private void printBase() {
        super.print(1);
    }

}
