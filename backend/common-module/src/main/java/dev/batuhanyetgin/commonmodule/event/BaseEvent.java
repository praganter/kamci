package dev.batuhanyetgin.commonmodule.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseEvent implements Serializable {

    @Builder.Default
    private long timestamp = System.currentTimeMillis();

    @Builder.Default
    private String locale = "tr";
}