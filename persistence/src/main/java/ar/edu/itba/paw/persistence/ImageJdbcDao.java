package ar.edu.itba.paw.persistence;
import ar.edu.itba.paw.interfaces.persistence.ImageDao;
import ar.edu.itba.paw.model.Image;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

//@Repository
public class ImageJdbcDao implements ImageDao {

    private final JdbcTemplate jdbc;
    private final SimpleJdbcInsert insert;

    private static final RowMapper<Image> ROW_MAPPER = (rs, i) -> new Image(
            rs.getLong("id"),
            rs.getString("filename"),
            rs.getString("content_type"),
            rs.getInt("size_bytes"),
            rs.getBytes("data")
    );


    public ImageJdbcDao(DataSource ds) {
        this.jdbc = new JdbcTemplate(ds);
        this.insert = new SimpleJdbcInsert(jdbc)
                .withTableName("image")
                .usingGeneratedKeyColumns("id");
    }

    @Override
    public Image insert(String filename, String contentType, int sizeBytes, byte[] data) {
        Number key = insert.executeAndReturnKey(Map.of(
                "filename", filename,
                "content_type", contentType,
                "size_bytes", sizeBytes,
                "data", data
        ));
        return new Image(key.longValue(), filename, contentType, sizeBytes, data);
    }

    @Override
    public Image insert(String filename, String contentType, int sizeBytes, byte[] data, long ownerId) {
        Number key = insert.executeAndReturnKey(Map.of(
                "filename", filename,
                "content_type", contentType,
                "size_bytes", sizeBytes,
                "data", data,
                "owner_id", ownerId,
                "created_at", LocalDateTime.now()
        ));
        return new Image(key.longValue(), filename, contentType, sizeBytes, data);
    }

    @Override
    public Optional<Image> findImageById(long id) {
        return jdbc.query("SELECT * FROM Image WHERE id = ?", ROW_MAPPER, id).stream().findFirst();
    }

    @Override
    public boolean deleteImage(long imageId) {
        final String sql = "DELETE FROM IMAGE WHERE id = ?";
        int rowsAffected = jdbc.update(sql, imageId);
        return rowsAffected > 0;
    }

    @Override
    public boolean deleteIfUnassigned(long imageId) {
        final String sql = """
                DELETE FROM IMAGE
                WHERE id = ?
                  AND NOT EXISTS (
                      SELECT 1 FROM ROOM WHERE ROOM.image_id = IMAGE.id
                  )
                """;
        return jdbc.update(sql, imageId) > 0;
    }

    @Override
    public int deleteUnassignedImagesOlderThan(LocalDateTime threshold) {
        final String sql = """
                DELETE FROM IMAGE
                WHERE owner_id IS NOT NULL
                  AND created_at < ?
                  AND NOT EXISTS (
                      SELECT 1 FROM ROOM WHERE ROOM.image_id = IMAGE.id
                  )
                """;
        return jdbc.update(sql, threshold);
    }

}
