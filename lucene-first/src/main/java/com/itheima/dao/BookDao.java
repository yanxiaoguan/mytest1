package com.itheima.dao;

import com.itheima.pojo.Book;

import java.util.List;

public interface BookDao {
    List<Book> findAllBooks();
}
