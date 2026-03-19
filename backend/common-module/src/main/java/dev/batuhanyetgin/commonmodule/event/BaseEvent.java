package dev.batuhanyetgin.commonmodule.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseEvent implements Serializable {
    private long timestamp = System.currentTimeMillis();
    private String locale = "tr";
}