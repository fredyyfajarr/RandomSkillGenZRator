package id.kelompok1.randomskillgen_zrator;

import androidx.annotation.Nullable;

public class Event<T> {

    private final T content;
    private boolean handled;

    public Event(T content) {
        this.content = content;
    }

    @Nullable
    public T getContentIfNotHandled() {
        if (handled) return null;
        handled = true;
        return content;
    }
}
