package me.teajay.cheeky.monkeys.entity;

/// The monkey's base pose, decided on the server and synced to clients to pick the looping animation.
/// Walking and running aren't states; they're derived from movement on the client.
public enum MonkeyState {
    IDLE,
    SITTING,
    SLEEPING,
    CLIMBING,
    HANGING,
    SWINGING,
    PERCHING,
    LEAPING,
    DANCING;

    private static final MonkeyState[] VALUES = values();

    public static MonkeyState byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : IDLE;
    }
}
