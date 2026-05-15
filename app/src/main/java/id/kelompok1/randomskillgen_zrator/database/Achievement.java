package id.kelompok1.randomskillgen_zrator.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "Achievement")
public class Achievement {

    @PrimaryKey
    @NonNull
    public String id;

    public String title;
    public String description;
    public String icon;
    public boolean unlocked;

    public Achievement(
            @NonNull String id,
            String title,
            String description,
            String icon,
            boolean unlocked
    ) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.icon = icon;
        this.unlocked = unlocked;
    }
}