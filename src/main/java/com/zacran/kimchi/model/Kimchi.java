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

	public List<String> ingredients;
	public int age;
	public KimchiRating rating;

}