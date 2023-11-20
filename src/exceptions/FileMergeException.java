package exceptions;

/** Custom exception for file merging */
public class FileMergeException extends Exception{

    /** Constructs file merge exception */
    public FileMergeException(String s) {
        super(s);
    }
}
