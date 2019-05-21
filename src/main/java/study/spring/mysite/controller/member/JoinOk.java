package study.spring.mysite.controller.member;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import study.spring.helper.FileInfo;
import study.spring.helper.RegexHelper;
import study.spring.helper.UploadHelper;
import study.spring.helper.WebHelper;
import study.spring.mysite.model.Member;
import study.spring.mysite.service.MemberService;

@Controller
public class JoinOk {
	
	/** (1) 사용하고자 하는 Helper + Service 객체 선언 */
	// --> import org.apache.logging.log4j.Logger;
	Logger logger = LoggerFactory.getLogger(JoinOk.class);
	// --> import org.apache.ibatis.session.SqlSession;
	@Autowired
	SqlSession sqlSession;
	// --> import study.jsp.helper.WebHelper;
	@Autowired
	WebHelper web;
	// --> import study.jsp.helper.RegexHelper;
	@Autowired
	RegexHelper regex;
	// --> import study.jsp.helper.UploadHelper;
	@Autowired
	UploadHelper upload;
	// --> import study.jsp.mysite.service.MemberService;
	@Autowired
	MemberService memberService;

	@RequestMapping(value = "member/join_ok.do")
	public ModelAndView doRun(Locale locale, Model model,
			HttpServletRequest request, HttpServletResponse response) 
					throws ServletException, IOException {

		/** (2) 사용하고자 하는 Helper+Service 객체 생성 */
		web.init();
		
		/** (3) 로그인 여부 검사 */
		// 로그인 중이라면 이 페이지를 동작시켜서는 안된다.
		if (web.getSession("loginInfo") != null) {
			// 이미 SqlSession 객체를 생성했으므로, 데이터베이스 접속을 해제해야 한다.
			sqlSession.close();
			return web.redirect(web.getRootPath() + "/index.do", "이미 로그인 하셨습니다.");
		}

		/** (4) 파일이 포함된 POST 파라미터 받기 */
		// <form>태그 안에 <input type="file">요소가 포함되어 있고,
		// <form>태그에 enctype="multipart/form-data"가 정의되어 있는 경우
		// WebHelper의 getString()|getInt() 메서드는 더 이상 사용할 수 없게 된다.
		try {
			upload.multipartRequest();
		} catch (Exception e) {
			sqlSession.close();
			return web.redirect(null, "multipart 데이터가 아닙니다.");
		}

		// UploadHelper에서 텍스트 형식의 파라미터를 분류한 Map을 리턴받아서 값을 추출한다.
		Map<String, String> paramMap = upload.getParamMap();
		String userId = paramMap.get("user_id");
		String userPw = paramMap.get("user_pw");
		String userPwRe = paramMap.get("user_pw_re");
		String name = paramMap.get("name");
		String email = paramMap.get("email");
		String tel = paramMap.get("tel");
		String birthdate = paramMap.get("birthdate");
		String gender = paramMap.get("gender");
		String postcode = paramMap.get("postcode");
		String addr1 = paramMap.get("addr1");
		String addr2 = paramMap.get("addr2");

		// 전달받은 파라미터는 값의 정상여부 확인을 위해서 로그로 확인
		logger.debug("userId=" + userId);
		logger.debug("userPw=" + userPw);
		logger.debug("userPwRe=" + userPwRe);
		logger.debug("name=" + name);
		logger.debug("email=" + email);
		logger.debug("tel=" + tel);
		logger.debug("birthdate=" + birthdate);
		logger.debug("gender=" + gender);
		logger.debug("postcode=" + postcode);
		logger.debug("addr1=" + addr1);
		logger.debug("addr2=" + addr2);

		/** (5) 입력값의 유효성 검사 */
		// 아이디 검사
		if (!regex.isValue(userId)) {
			sqlSession.close();
			return web.redirect(null, "아이디를 입력하세요.");
		}

		if (!regex.isEngNum(userId)) {
			sqlSession.close();
			return web.redirect(null, "아이디는 숫자와 영문의 조합으로 20자까지만 가능합니다.");
		}

		if (userId.length() > 20) {
			sqlSession.close();
			return web.redirect(null, "아이디는 숫자와 영문의 조합으로 20자까지만 가능합니다.");
		}

		// 비밀번호 검사
		if (!regex.isValue(userPw)) {
			sqlSession.close();
			return web.redirect(null, "비밀번호를 입력하세요.");
		}

		if (!regex.isEngNum(userPw)) {
			sqlSession.close();
			return web.redirect(null, "비밀번호는 숫자와 영문의 조합으로 20자까지만 가능합니다.");
		}

		if (userPw.length() > 20) {
			sqlSession.close();
			return web.redirect(null, "비밀번호는 숫자와 영문의 조합으로 20자까지만 가능합니다.");
		}

		// 비밀번호 확인
		if (!userPw.equals(userPwRe)) {
			sqlSession.close();
			return web.redirect(null, "비밀번호 확인이 잘못되었습니다.");
		}

		// 이름 검사
		if (!regex.isValue(name)) {
			sqlSession.close();
			return web.redirect(null, "이름을 입력하세요.");
		}

		if (!regex.isKor(name)) {
			sqlSession.close();
			return web.redirect(null, "이름은 한글만 입력 가능합니다.");
		}

		if (name.length() < 2 || name.length() > 5) {
			sqlSession.close();
			return web.redirect(null, "이름은 2~5글자 까지만 가능합니다.");
		}

		// 이메일 검사
		if (!regex.isValue(email)) {
			sqlSession.close();
			return web.redirect(null, "이메일을 입력하세요.");
		}

		if (!regex.isEmail(email)) {
			sqlSession.close();
			return web.redirect(null, "이메일의 형식이 잘못되었습니다.");
		}

		// 연락처 검사
		if (!regex.isValue(tel)) {
			sqlSession.close();
			return web.redirect(null, "연락처를 입력하세요.");
		}

		if (!regex.isCellPhone(tel) && !regex.isTel(tel)) {
			sqlSession.close();
			return web.redirect(null, "연락처의 형식이 잘못되었습니다.");
		}

		// 생년월일 검사
		if (!regex.isValue(birthdate)) {
			sqlSession.close();
			return web.redirect(null, "생년월일을 입력하세요.");
		}

		// 성별검사
		if (!regex.isValue(gender)) {
			sqlSession.close();
			return web.redirect(null, "성별을 입력하세요.");
		}

		if (!gender.equals("M") && !gender.equals("F")) {
			sqlSession.close();
			return web.redirect(null, "성별이 잘못되었습니다.");
		}

		/** (6) 업로드 된 파일 정보 추출 */
		List<FileInfo> fileList = upload.getFileList();
		// 업로드 된 프로필 사진 경로가 저장될 변수
		String profileImg = null;

		// 업로드 된 파일이 존재할 경우만 변수값을 할당한다.
		if (fileList.size() > 0) {
			// 단일 업로드이므로 0번째 항목만 가져온다.
			FileInfo info = fileList.get(0);
			profileImg = info.getFileDir() + "/" + info.getFileName();
		}

		// 파일경로를 로그로 기록
		logger.debug("profileImg=" + profileImg);

		/** (7) 전달받은 파라미터를 Beans 객체에 담는다. */
		Member member = new Member();
		member.setUserId(userId);
		member.setUserPw(userPwRe);
		member.setName(name);
		member.setEmail(email);
		member.setTel(tel);
		member.setBirthdate(birthdate);
		member.setGender(gender);
		member.setPostcode(postcode);
		member.setAddr1(addr1);
		member.setAddr2(addr2);
		member.setProfileImg(profileImg);

		/** (8) Service를 통한 데이터베이스 저장 처리 */
		try {
			memberService.insertMember(member);
		} catch (Exception e) {
			sqlSession.close();
			return web.redirect(null, e.getLocalizedMessage());
		}

		/** (9) 가입이 완료되었으므로 메인페이지로 이동 */
		return web.redirect(web.getRootPath() + "/index.do", "회원가입이 완료되었습니다. 로그인 해 주세요.");

	}

}
