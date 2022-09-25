package pt.ua.imodec.datastructs;

import java.util.Iterator;

public class MergeIterator<T> implements Iterator<T> {

    private final Iterator<T> it1, it2;

    public MergeIterator(Iterator<T> it1, Iterator<T> it2) {
        this.it1 = it1;
        this.it2 = it2;
    }

    @Override
    public boolean hasNext() {
        return it1.hasNext() || it2.hasNext();
    }

    @Override
    public T next() {
        if (it1.hasNext())
            return it1.next();
        return it2.next();
    }
}
