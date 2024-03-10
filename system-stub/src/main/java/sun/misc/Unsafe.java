package sun.misc;

public class Unsafe {
    public static Unsafe getUnsafe() {
        throw new RuntimeException();
    }

    /**
     * Gets {@code int} from given address in memory.
     *
     * @param address address in memory
     * @return {@code int} value
     */
    public int getInt(long address) {
        throw new RuntimeException("");
    }

    /**
     * Stores a {@code int} into the given memory address.
     *
     * @param address  address in memory where to store the value
     * @param newValue the value to store
     */
    public void putInt(long address, int newValue) {
        throw new RuntimeException("");
    }

}

