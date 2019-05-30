package com.zacran.kimchi.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Kimchi {

	List<String> ingredients;
	int age;
	KimchiRating rating;

}