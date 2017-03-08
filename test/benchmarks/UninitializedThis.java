
package benchmarks;

/**
 * @author Rohan Padhye
 */
public class UninitializedThis extends  Wrapper {


    public UninitializedThis() {

    }

    public UninitializedThis(String s) {
        super(s);
    }

    public static void main(String[] args) {
        if (args.length > 0)
            new UninitializedThis(args[0]);
        else
            new UninitializedThis();
    }


}
