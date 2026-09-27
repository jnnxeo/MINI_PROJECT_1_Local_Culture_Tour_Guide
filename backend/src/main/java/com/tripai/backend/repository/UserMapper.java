package com.tripai.backend.repository;

import com.tripai.backend.domain.entity.User;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper {

	Optional<User> findByEmail(String email);

	int insert(User user);
}
