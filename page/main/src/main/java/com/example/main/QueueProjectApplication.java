package com.example.main;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;

@SpringBootApplication
@Controller
public class QueueProjectApplication {

	RestTemplate restTemplate = new RestTemplate();


	public static void main(String[] args) {
		SpringApplication.run(QueueProjectApplication.class, args);
	}

	@GetMapping("/")
	public String index(@RequestParam(name="queue",defaultValue = "default") String queue,
						@RequestParam(name="user_id") Long userId,
						HttpServletRequest request){
		var cookies = request.getCookies();
		var cookieName = "user-queue-%s-token".formatted(queue);

		String token="";
		if(cookies!=null){
			var cookie = Arrays.stream(cookies).filter(i -> i.getName().equalsIgnoreCase(cookieName)).findFirst();
			token = cookie.orElse(new Cookie(cookieName,"")).getValue();
		}

		var uri = UriComponentsBuilder
				.fromUriString("http://flow-service:8080")
				.path("/api/v1/queue/allowed")
				.queryParam("queue",queue)
				.queryParam("user_id",userId)
				.queryParam("token",token)
				.encode().build().toUri();
		ResponseEntity<AllowedUserResponse> response = restTemplate.getForEntity(uri,AllowedUserResponse.class);
		if(response.getBody()==null || !response.getBody().allowed()){
			return "redirect:http://localhost:9010/waiting-room?user_id=%d&redirect_url=%s"
					.formatted(userId,"http://localhost:9000?user_id=%d".formatted(userId));
		}
		return "index";
	}

	public record AllowedUserResponse(Boolean allowed){}
}
