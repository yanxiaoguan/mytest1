package com.itheima.dao.impl;

import com.itheima.dao.BookDao;
import com.itheima.pojo.Book;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BookDaoImpl implements BookDao {
    public List<Book> findAllBooks() {
        List<Book> bookList = new ArrayList<Book>();
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet rs = null;
        try {
            //加载驱动
            Class.forName("com.mysql.jdbc.Driver");
            //获取连接
             connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/test","root","root");
            //编写sql
            String sql = "select * from book";
            //创建prepareStatement
            preparedStatement = connection.prepareStatement(sql);
            //执行预编译
            rs = preparedStatement.executeQuery();
            //迭代ResultSet
            while(rs.next()){
                Book book = new Book();
                // 图书id
                book.setId(rs.getInt("id"));
                // 图书名称
                book.setBookname(rs.getString("bookname"));
                // 图书价格
                book.setPrice(rs.getFloat("price"));
                // 图书图片
                book.setPic(rs.getString("pic"));
                // 图书描述
                book.setBookdesc(rs.getString("bookdesc"));

                //添加到集合
                bookList.add(book);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            // 释放资源
            try {
                if(rs !=null) rs.close();
                if(preparedStatement !=null) preparedStatement.close();
                if(connection !=null) connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return bookList;
    }
}
