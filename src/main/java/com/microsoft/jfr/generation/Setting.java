package com.microsoft.jfr.generation;

class Setting {
    final String name;
    final String value;

    public Setting(String name, String value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public String toString() {
        return "Setting{" +
                "name='" + name + '\'' +
                ", value='" + value + '\'' +
                '}';
    }

}
