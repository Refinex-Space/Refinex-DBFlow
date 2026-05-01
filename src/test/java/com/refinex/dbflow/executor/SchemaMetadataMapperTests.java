package com.refinex.dbflow.executor;

import com.refinex.dbflow.executor.dto.SchemaIndexMetadata;
import com.refinex.dbflow.executor.support.SchemaMetadataMapper;
import org.junit.jupiter.api.Test;

import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetMetaDataImpl;
import javax.sql.rowset.RowSetProvider;
import java.sql.SQLException;
import java.sql.Types;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link SchemaMetadataMapper} 单元测试。
 *
 * @author refinex
 */
class SchemaMetadataMapperTests {

    /**
     * 验证 NON_UNIQUE 为空时不会因为自动拆箱触发空指针异常。
     *
     * @throws SQLException RowSet 构造失败时抛出
     */
    @Test
    void shouldMapIndexWhenNonUniqueIsNull() throws SQLException {
        CachedRowSet rowSet = RowSetProvider.newFactory().createCachedRowSet();
        rowSet.setMetaData(indexMetadata());
        rowSet.moveToInsertRow();
        rowSet.updateString(1, "demo");
        rowSet.updateString(2, "orders");
        rowSet.updateString(3, "idx_orders_status");
        rowSet.updateNull(4);
        rowSet.updateInt(5, 1);
        rowSet.updateString(6, "status");
        rowSet.updateString(7, "BTREE");
        rowSet.updateLong(8, 100L);
        rowSet.updateString(9, "YES");
        rowSet.updateString(10, "comment");
        rowSet.updateString(11, "index comment");
        rowSet.insertRow();
        rowSet.moveToCurrentRow();
        rowSet.beforeFirst();
        rowSet.next();

        SchemaIndexMetadata metadata = SchemaMetadataMapper.index(rowSet);

        assertThat(metadata.schemaName()).isEqualTo("demo");
        assertThat(metadata.tableName()).isEqualTo("orders");
        assertThat(metadata.name()).isEqualTo("idx_orders_status");
        assertThat(metadata.nonUnique()).isFalse();
        assertThat(metadata.unique()).isTrue();
        assertThat(metadata.seqInIndex()).isEqualTo(1);
        assertThat(metadata.columnName()).isEqualTo("status");
    }

    /**
     * 创建索引结果集元数据。
     *
     * @return RowSet 元数据
     * @throws SQLException 创建失败时抛出
     */
    private RowSetMetaDataImpl indexMetadata() throws SQLException {
        RowSetMetaDataImpl metadata = new RowSetMetaDataImpl();
        metadata.setColumnCount(11);
        metadata.setColumnName(1, "TABLE_SCHEMA");
        metadata.setColumnType(1, Types.VARCHAR);
        metadata.setColumnName(2, "TABLE_NAME");
        metadata.setColumnType(2, Types.VARCHAR);
        metadata.setColumnName(3, "INDEX_NAME");
        metadata.setColumnType(3, Types.VARCHAR);
        metadata.setColumnName(4, "NON_UNIQUE");
        metadata.setColumnType(4, Types.INTEGER);
        metadata.setColumnName(5, "SEQ_IN_INDEX");
        metadata.setColumnType(5, Types.INTEGER);
        metadata.setColumnName(6, "COLUMN_NAME");
        metadata.setColumnType(6, Types.VARCHAR);
        metadata.setColumnName(7, "INDEX_TYPE");
        metadata.setColumnType(7, Types.VARCHAR);
        metadata.setColumnName(8, "CARDINALITY");
        metadata.setColumnType(8, Types.BIGINT);
        metadata.setColumnName(9, "NULLABLE");
        metadata.setColumnType(9, Types.VARCHAR);
        metadata.setColumnName(10, "COMMENT");
        metadata.setColumnType(10, Types.VARCHAR);
        metadata.setColumnName(11, "INDEX_COMMENT");
        metadata.setColumnType(11, Types.VARCHAR);
        return metadata;
    }
}

