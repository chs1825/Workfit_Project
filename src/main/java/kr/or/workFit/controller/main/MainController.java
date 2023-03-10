package kr.or.workFit.controller.main;

import static dev.samstevens.totp.util.Utils.getDataUriForImage;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.SessionAttribute;

import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import kr.or.workFit.service.main.MainService;
import kr.or.workFit.service.mypage.MyWorkService;
import kr.or.workFit.service.notification.NotificationService;
import kr.or.workFit.vo.MemberVO;
import kr.or.workFit.vo.NotificationVO;
import kr.or.workFit.vo.WorkVO;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class MainController  {
	
	@Inject
	MainService service;
	
	@Inject
	MyWorkService workservice;
	
	@Inject
	NotificationService notificationService;
	
	
	@GetMapping("/test")
	public String test(Model model) {
		return "workfit/main/noTiles/test";
	}
	
	@GetMapping("/login")
	public String login() {
		log.info("login");
		return "workfit/main/noTiles/login";
	}
	
	@GetMapping("/loginSecurityError")
	public String loginError() {
		log.info("loginError");
		return "workfit/errorPage/LoginSecurityError";
	}
	@GetMapping("/workfit/QRCheck")
	public String loginCheck(@SessionAttribute("member") MemberVO member, Model model, HttpServletRequest req) {
		log.info("QRCheck");
		
		String usrId = member.getMemId();
		Map<String, Object> resultMap = new HashMap<String, Object>();
//		MemberVO updateMember = (MemberVO) session.getAttribute("member");

		SecretGenerator secretGenerator = new DefaultSecretGenerator();
		String secret = secretGenerator.generate();

		QrData data = new QrData.Builder()
				.label(usrId)
				.secret(secret)
				.issuer("Application Name")
				.algorithm(HashingAlgorithm.SHA1)
				.digits(6)
				.period(30)
				.build();
//		System.out.println(data.getSecret());
		
		QrGenerator qrGenerator = new ZxingPngQrGenerator();
		byte[] imageData = null;
		try {
			imageData = qrGenerator.generate(data);
		}
		catch (QrGenerationException e) {
			// QR ?????? ?????? ?????? ??????
//			return new ResponseEntity<>(resultMap, HttpStatus.BAD_REQUEST);
			e.printStackTrace();
		}

		String mimeType = qrGenerator.getImageMimeType();
		String dataUri = getDataUriForImage(imageData, mimeType);
		resultMap.put("encodedKey", secret);
		resultMap.put("url", dataUri);
		resultMap.put("memName", member.getMemName());
		model.addAttribute("resultMap",resultMap);
		
		return "workfit/main/noTiles/qr";
	}
	
//	@GetMapping("/logout")
//	public String logout(HttpServletRequest req) {
//		HttpSession session = req.getSession();
//		session.invalidate();
//		return "workfit/main/noTiles/login";
//	}
	
	@PostMapping("/logout")
	public String logout(HttpServletRequest req) {
		HttpSession session = req.getSession();
		session.invalidate();
		return "workfit/main/noTiles/login";
	}
	
	@GetMapping("/qr")
	public String qr() {
		log.info("qr");
		return "workfit/main/noTiles/qr";
	}
	
	@GetMapping("/workfit/main")
	public String main(@SessionAttribute("memId") String memId, 
			String flag, Model model, @SessionAttribute("memCo")String memCo,
			@SessionAttribute("memDeptName")String memDeptName) throws Exception{
		log.info("main");
		
		// ?????? ????????? ????????? ??????
//		int serviceInfo = Integer.parseInt(service.getMemberShipInfo(memCo).trim());
//		model.addAttribute("serviceInfo", serviceInfo);
//		log.debug("????????? ?????? ?????? ? , {}",serviceInfo);
		
		model.addAttribute("commuteFlag",flag);
		
		// ????????? ?????? ???????????? ????????? ?????? ?????? 
		model.addAttribute("memIdForAlarm",memId);
		
		// ?????? ?????? ??????????????? ???????????? ????????? ???????????? ??? ?????? ???????????? ????????????     
		List<NotificationVO> notVOList = notificationService.selectReceivedNotification(memId);
		// ???????????? ????????? ???????????? ??? ????????? ??? ?????? ????????? false ??? ???????????? ?????? ?????? ???????????? ???????????? ???????????? ????????? ?????? ??? 
		
		
		Map<String,String> senderProfileMap = new HashMap<String, String>();
		
		
		List<NotificationVO> uncheckedNotList = new ArrayList<NotificationVO>();
		
		for(NotificationVO notVO : notVOList) {
			System.out.println("????????? notVO ?????? ??????"+notVO.getNotSendedDate());
			if("false".equals(notVO.getNotIsread())) {
				uncheckedNotList.add(notVO);
				
			}
			senderProfileMap.put(notVO.getNotSender(),notificationService.selectSenderProfile(notVO.getNotSender()));
			
		}
		model.addAttribute("notVOList",notVOList);
		model.addAttribute("uncheckedNotList",uncheckedNotList);
		
		model.addAttribute("senderProfileMap",senderProfileMap);
		
		
		System.out.println(flag);
		
		
		model.addAttribute("jjebg", "jjebg");
		model.addAttribute("memDeptName", memDeptName);
		
		
		int serviceInfo = Integer.parseInt(service.getMemberShipInfo(memCo).trim());
		model.addAttribute("serviceInfo", serviceInfo);
		
		
		return "workfit/main/mainTEST5";
	}
	

	/**
	 * ?????? ?????? ???????????? ???????????? ?????? 
	 * @param model
	 * @return
	 */
	@GetMapping("/workfit/openingTest")
	public String mainTest(Model model) {
		return "workfit/main/noTiles/openingTest";
	}
	
	/*
	 * ?????? ???????????????
	 * */
	@GetMapping("/workfits")
	public String mainTest2(Model model) {
		return "workfit/main/noTiles/opening";
	}
	
	/**
	 * ?????? ???????????? ???????????? ???????????? test2?????? 
	 * @param model
	 * @return
	 */
	@GetMapping("/workfit/main2")
	public String mainTEST2(Model model) {
		log.info("main2");
		model.addAttribute("jjebg", "jjebg");
		return "workfit/main/mainTEST2";
	}
	
	/**
	 * ?????? ???????????? ???????????? ???????????? main3??????
	 * @param model
	 * @return
	 */
	@GetMapping("/workfit/main1")
	public String mainTEST3(Model model) {
		log.info("main2");
		return "workfit/main/mainTEST";
	}
	/**
	 * db?????? ???????????? ???????????? ????????? 
	 * @param model
	 * @return
	 */
	@GetMapping("/workfit/main3")
	public String mainTEST4(@SessionAttribute("memDeptName") String memDeptName,
			Model model,@SessionAttribute("memCo")String memCo) throws Exception {
		log.info("main4");
		// ?????? ????????? ????????? ??????
		int serviceInfo = Integer.parseInt(service.getMemberShipInfo(memCo).trim());
		model.addAttribute("serviceInfo", serviceInfo);
		model.addAttribute("jjebg", "jjebg");
		model.addAttribute("memDeptName", memDeptName);
		return "workfit/main/mainTEST3";
	}
	@GetMapping("/workfit/main5")
	public String mainTEST5(@SessionAttribute("memDeptName") String memDeptName,
			Model model,@SessionAttribute("memCo")String memCo) throws Exception {
		log.info("main4");
		// ?????? ????????? ????????? ??????
		int serviceInfo = Integer.parseInt(service.getMemberShipInfo(memCo).trim());
		model.addAttribute("serviceInfo", serviceInfo);
		model.addAttribute("jjebg", "jjebg");
		model.addAttribute("memDeptName", memDeptName);
		return "workfit/main/mainTEST5";
	}
	
	@GetMapping("/workfit/main4")
	public String mainTEST5(@SessionAttribute("memDeptName") String memDeptName,Model model) {
		log.info("main5");
		model.addAttribute("jjebg", "jjebg");
		model.addAttribute("memDeptName", memDeptName);
		return "workfit/main/mainTEST4";
	}
	
	
	
	@PostMapping(value = "/otp/generate", produces = { MediaType.APPLICATION_JSON_UTF8_VALUE })
	public ResponseEntity<Map<String, Object>> createOtp(Locale locale, String memId, HttpServletRequest request, HttpServletResponse response, HttpSession session) {

		String usrId = memId;
		Map<String, Object> resultMap = new HashMap<String, Object>();
//		MemberVO updateMember = (MemberVO) session.getAttribute("member");

		SecretGenerator secretGenerator = new DefaultSecretGenerator();
		String secret = secretGenerator.generate();

		QrData data = new QrData.Builder()
				.label(usrId)
				.secret(secret)
				.issuer("Application Name")
				.algorithm(HashingAlgorithm.SHA1)
				.digits(6)
				.period(30)
				.build();
		System.out.println(data.getSecret());
		
		QrGenerator qrGenerator = new ZxingPngQrGenerator();
		byte[] imageData;
		try {
			imageData = qrGenerator.generate(data);
		}
		catch (QrGenerationException e) {
			// QR ?????? ?????? ?????? ??????
			return new ResponseEntity<>(resultMap, HttpStatus.BAD_REQUEST);
		}

		String mimeType = qrGenerator.getImageMimeType();
		String dataUri = getDataUriForImage(imageData, mimeType);
		resultMap.put("encodedKey", secret);
		resultMap.put("url", dataUri);
		return new ResponseEntity<>(resultMap, HttpStatus.OK);
	}
	
	
	@PostMapping(value = "/otp/verify")
	public ResponseEntity<String> verifyOtp(Locale locale,@RequestBody Map<String, Object> qrMap, @SessionAttribute("memId")String memId,HttpServletRequest req) {
		System.out.println("??????????");
		System.out.println("key??? : "+qrMap.get("encodedKey"));
		System.out.println("otpNum??? : "+qrMap.get("otpNumber"));
		
		
		TimeProvider timeProvider = new SystemTimeProvider();
		CodeGenerator codeGenerator = new DefaultCodeGenerator();
		CodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
		
		
		boolean check = verifier.isValidCode(qrMap.get("encodedKey").toString(), qrMap.get("otpNumber").toString());
		
		ResponseEntity<String> entity = null;
		if (check) {
			// OTP??? ????????? ?????? ??????.
			entity = new ResponseEntity<String>("success", HttpStatus.OK);
			
			
			LocalTime time = LocalTime.now();
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

			  
			  
			req.getSession().setAttribute("commuteState", "success");
			req.getSession().setAttribute("commuteStartClock",time.format(formatter));
			
			// ?????? ??? ???????????? ???????????? ?????? ????????? ?????? 
			WorkVO workVO = new WorkVO();
			
			workVO.setMemId(memId);
			workVO.setRestTime(1);
			workservice.insertCommuteStart(workVO);
			
			
		}else {
			entity = new ResponseEntity<String>("fail", HttpStatus.OK);
		}
		
		
		
		return entity;
	}
	
	@PostMapping(value = "/otp/commuteEnd")
	public void commuteEnd(@SessionAttribute("memId")String memId, String anything, HttpServletRequest req) {
		System.out.println("commuteEnd ?????????????????");
		workservice.endCommute(memId);
		
		req.getSession().setAttribute("commuteState","end");
		
		System.out.println("????????? ??? ??????????????? ??????"+req.getSession().getAttribute("commuteState").toString());
		
		LocalTime time = LocalTime.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

		req.getSession().setAttribute("commuteEndClock",time.format(formatter));
		
	}
	
	
	
}
