package ngz.extraction.filesource.adapter.example;

public interface DataGenerator<T> {
    default void open() {}

    T generate();

    Class<T> getType();
}
