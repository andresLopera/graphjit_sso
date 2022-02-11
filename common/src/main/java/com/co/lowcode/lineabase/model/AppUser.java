package com.co.lowcode.lineabase.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.Table;

@Entity
@Table(name = "app_user")
public class AppUser {

	@EmbeddedId
	AppUserKey id;
	
	
	@ManyToOne
    @MapsId("appId")
    @JoinColumn(name = "app_id")
    Group group;

    @ManyToOne
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    User user;
	
	
	//ADMIN, READ, WRITE
	private String profile;
	private Date created_at;
	
	public Date getCreated_at() {
		return created_at;
	}
	public void setCreated_at(Date created_at) {
		this.created_at = created_at;
	}
	public String getProfile() {
		return profile;
	}
	public void setProfile(String profile) {
		this.profile = profile;
	}
	public AppUserKey getId() {
		return id;
	}
	public void setId(AppUserKey id) {
		this.id = id;
	}
	

	public User getUser() {
		return user;
	}
	public void setUser(User user) {
		this.user = user;
	}
}