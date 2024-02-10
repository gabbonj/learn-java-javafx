package com.nbicocchi.javafx.planes.persistence.model;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Plane {
    Long Id;
    String name;
    Double length;
    Double wingspan;
    LocalDate firstFlight;
    String category;
    Set<Part> parts;

    public Plane() {
        this.parts = new HashSet<>();
    }

    public Plane(Long Id, String name, Double length, Double wingspan, LocalDate firstFlight, String category) {
        this.Id = Id;
        this.name = name;
        this.length = length;
        this.wingspan = wingspan;
        this.firstFlight = firstFlight;
        this.category = category;
        this.parts = new HashSet<>();
    }

    public Plane(String name, Double length, Double wingspan, LocalDate firstFlight, String category) {
        this.Id = null;
        this.name = name;
        this.length = length;
        this.wingspan = wingspan;
        this.firstFlight = firstFlight;
        this.category = category;
        this.parts = new HashSet<>();
    }

    public Long getId() {
        return Id;
    }

    public void setId(Long id) {
        Id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getLength() {
        return length;
    }

    public void setLength(Double length) {
        this.length = length;
    }

    public Double getWingspan() {
        return wingspan;
    }

    public void setWingspan(Double wingspan) {
        this.wingspan = wingspan;
    }

    public LocalDate getFirstFlight() {
        return firstFlight;
    }

    public void setFirstFlight(LocalDate firstFlight) {
        this.firstFlight = firstFlight;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Set<Part> getParts() {
        return parts;
    }

    public void addPart(Part part) {
        this.parts.add(part);
        part.setPlane(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Plane plane = (Plane) o;
        return Objects.equals(Id, plane.Id) && Objects.equals(name, plane.name) && Objects.equals(length, plane.length) && Objects.equals(wingspan, plane.wingspan) && Objects.equals(firstFlight, plane.firstFlight) && Objects.equals(category, plane.category) && Objects.equals(parts, plane.parts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Id, name, length, wingspan, firstFlight, category, parts);
    }

    @Override
    public String toString() {
        return "Plane{" + "Id=" + Id + ", name='" + name + '\'' + ", length=" + length + ", wingspan=" + wingspan + ", firstFlight=" + firstFlight + ", category='" + category + '\'' + ", parts=" + parts + '}';
    }
}