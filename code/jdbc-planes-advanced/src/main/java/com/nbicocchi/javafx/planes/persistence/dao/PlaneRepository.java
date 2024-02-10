package com.nbicocchi.javafx.planes.persistence.dao;

import com.nbicocchi.javafx.planes.persistence.model.Part;
import com.nbicocchi.javafx.planes.persistence.model.Plane;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlaneRepository implements Repository<Plane, Long> {
    private static final Logger LOG = LoggerFactory.getLogger(PlaneRepository.class);
    private final HikariDataSource dataSource;

    public PlaneRepository(HikariDataSource dataSource) {
        this.dataSource = dataSource;
        if (!isTableFound("planes") || !isTableFound("parts")) {
            initTable();
        }
    }

    private boolean isTableFound(String tableName) {
        LOG.info("Checking table {}", tableName);
        String sql = String.format("SELECT * FROM %s LIMIT 1", tableName);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeQuery();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    private void initTable() {
        LOG.info("Initializing table");
        String sql = """
                drop table if exists planes, parts;
                create table planes(
                    id serial,
                    name varchar(50) default null,
                    length double precision default null,
                    wingspan double precision default null,
                    firstflight date default null,
                    category varchar(50) default null,
                    primary key (id));
                create table parts(
                    id serial,
                    planeid bigint default null,
                    partcode varchar(50) default null,
                    description varchar(50) default null,
                    duration double precision default null,
                    primary key (id),
                    foreign key (planeid) references planes(id));
                """;
        try (Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public Optional<Plane> findById(Long Id) {
        Optional<Plane> optionalPlane = findPlaneById(Id);
        if (optionalPlane.isPresent()) {
            Set<Part> parts = findPartsByPlane(optionalPlane.get());
            for (Part part : parts) {
                optionalPlane.get().addPart(part);
            }
        }
        return optionalPlane;
    }

    @Override
    public Iterable<Plane> findAll() {
        Iterable<Plane> planes = findPlaneAll();
        for (Plane plane : planes) {
            Set<Part> parts = findPartsByPlane(plane);
            for (Part part : parts) {
                plane.addPart(part);
            }
        }
        return planes;
    }

    @Override
    public Plane save(Plane entity) {
        if (Objects.isNull(entity.getId())) {
            Plane saved = insertPlane(entity);
            insertPartsByPlane(saved);
            return saved;
        }

        Optional<Plane> plane = findById(entity.getId());
        if (plane.isEmpty()) {
            Plane saved = insertPlane(entity);
            insertPartsByPlane(saved);
            return saved;
        } else {
            Plane saved = updatePlane(entity);
            deletePartsByPlaneId(saved.getId());
            insertPartsByPlane(saved);
            return saved;
        }
    }

    @Override
    public void delete(Plane entity) {
        deletePlane(entity);
        deletePartsByPlaneId(entity.getId());
    }

    @Override
    public void deleteById(Long Id) {
        deletePlaneById(Id);
        deletePartsByPlaneId(Id);
    }

    @Override
    public void deleteAll() {
        deletePlaneAll();
        deletePartsAll();
    }

    private Plane insertPlane(Plane entity) {
        LOG.info("Executing insertPlane()");
        String sql = "INSERT INTO planes (name, length, wingspan, firstflight, category) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, entity.getName());
            statement.setDouble(2, entity.getLength());
            statement.setDouble(3, entity.getWingspan());
            statement.setDate(4, Date.valueOf(entity.getFirstFlight()));
            statement.setString(5, entity.getCategory());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                entity.setId(keys.getLong(1));
                return entity;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private Plane updatePlane(Plane entity) {
        LOG.info("Executing updatePlane()");
        String sql = "UPDATE planes SET name=?, length=?, wingspan=?, firstflight=?, category=? WHERE id=?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, entity.getName());
            statement.setDouble(2, entity.getLength());
            statement.setDouble(3, entity.getWingspan());
            statement.setDate(4, Date.valueOf(entity.getFirstFlight()));
            statement.setString(5, entity.getCategory());
            statement.setLong(6, entity.getId());
            statement.executeUpdate();
            return entity;
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private void deletePlane(Plane entity) {
        deleteById(entity.getId());
    }

    private void deletePlaneById(Long Id) {
        LOG.info("Executing deletePlaneById()");
        String sql = "DELETE FROM planes WHERE id=?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, Id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private void deletePlaneAll() {
        LOG.info("Executing deletePlaneAll()");
        String sql = "DELETE FROM planes";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private LocalDate convertSQLDateToLocalDate(Date SQLDate) {
        java.util.Date date = new java.util.Date(SQLDate.getTime());
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private Optional<Plane> findPlaneById(Long Id) {
        LOG.info("Executing findPlaneById()");
        String sql = "SELECT * FROM planes WHERE id=?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, Id);
            ResultSet rs = statement.executeQuery();

            if (!rs.next()) {
                return Optional.empty();
            }

            return Optional.of(new Plane(rs.getLong("id"), rs.getString("name"), rs.getDouble("length"), rs.getDouble("wingspan"), convertSQLDateToLocalDate(rs.getDate("firstflight")), rs.getString("category")));
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private Set<Plane> findPlaneAll() {
        LOG.info("Executing findPlaneAll()");
        String sql = "SELECT * FROM planes";
        Set<Plane> planeSet = new HashSet<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                planeSet.add(new Plane(rs.getLong("id"), rs.getString("name"), rs.getDouble("length"), rs.getDouble("wingspan"), convertSQLDateToLocalDate(rs.getDate("firstflight")), rs.getString("category")));
            }
            return planeSet;
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private Set<Part> findPartsByPlane(Plane plane) {
        LOG.info("Executing findPartsByPlane()");
        String sql = "SELECT * FROM parts WHERE planeid=?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, plane.getId());
            ResultSet rs = statement.executeQuery();

            Set<Part> parts = new HashSet<>();
            while (rs.next()) {
                parts.add(new Part(
                        rs.getLong("id"),
                        null,
                        rs.getString("partcode"),
                        rs.getString("description"),
                        rs.getDouble("duration")
                ));
            }
            return parts;
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private void insertPartsByPlane(Plane entity) {
        LOG.info("Executing insertPartsByPlane()");
        for (Part part : entity.getParts()) {
            String sql = "INSERT INTO parts (planeid, partcode, description, duration) VALUES (?, ?, ?, ?)";
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, entity.getId());
                statement.setString(2, part.getPartCode());
                statement.setString(3, part.getDescription());
                statement.setDouble(4, part.getDuration());
                statement.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e.getMessage());
            }
        }
    }

    private void deletePartsByPlaneId(Long Id) {
        LOG.info("Executing deletePartsByPlaneId()");
        String sql = "DELETE FROM parts WHERE planeid=?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, Id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private void deletePartsAll() {
        LOG.info("Executing deletePartsAll()");
        String sql = "DELETE FROM parts";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}