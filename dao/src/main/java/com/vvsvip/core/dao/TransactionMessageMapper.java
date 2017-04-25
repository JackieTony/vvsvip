package com.vvsvip.core.dao;

import com.vvsvip.core.model.TransactionMessage;

public interface TransactionMessageMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TransactionMessage record);

    int insertSelective(TransactionMessage record);

    TransactionMessage selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TransactionMessage record);

    int updateByPrimaryKey(TransactionMessage record);
}