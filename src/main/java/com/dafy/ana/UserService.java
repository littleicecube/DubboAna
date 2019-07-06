package com.dafy.ana;

public class UserService implements IUserService{

	public String getUserName(long lUserId) {
		return "userName"+lUserId;
	}

}
