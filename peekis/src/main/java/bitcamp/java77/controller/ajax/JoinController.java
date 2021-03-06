package bitcamp.java77.controller.ajax;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import bitcamp.java77.dao.JoinDao;
import bitcamp.java77.domain.AjaxResult;
import bitcamp.java77.domain.Join;

@Controller("ajax.JoinController")
@RequestMapping("/auth/ajax/*")
public class JoinController {
	static Logger logger = Logger.getLogger(FileuploadController.class);
	public static final String SAVED_DIR = "/attachfile";

	@Autowired
	JoinDao joinDao;
	@Autowired
	ServletContext servletContext;

	// 회원가입
	@RequestMapping(value = "join", method = RequestMethod.POST)
	public AjaxResult join(Join join, HttpSession session) throws Exception {

		System.out.println("컨트롤러 호출 : " + join.getSelList());

		// user table 에 등록
		joinDao.memberJoin(join);

		// 등록된 유저의 번호를 가져옴
		int joinUserNo = joinDao.selectNo();
		join.setuNo(joinUserNo);
		System.out.println("등록된 유저의 유저번호  : " + joinUserNo);

		// Join.getSelList를 # 구분자로 파싱해서 유저엔태그 테이블에 등록된 유저번호와 함께 등록하
		String tagStr = join.getSelList().replaceFirst("#", "");
		tagStr = tagStr.trim();
		String[] tagList = tagStr.split("#");

		// 넘어온 태그번호 확인
		for (int i = 0; i < tagList.length; i++) {
			System.out.print((i + 1) + " 번째 태그번호 : " + tagList[i] + "\n");
		}

		join.setUtNo(joinUserNo);
		for (int i = 0; i < tagList.length; i++) {
			join.settNo(Integer.parseInt(tagList[i]));
			joinDao.registTag(join);
		}
		// 디폴트 카테고리 생성
		join.setcName("기본폴더");
		joinDao.addCategory(join);

		// 등록된 유저를 세션에 등록
		session.setAttribute("loginUser", join);
		System.out.println(join.toString());
		return new AjaxResult("success", null);
	}

	// 이메일 중복체크
	@RequestMapping(value = "loginCk", method = RequestMethod.POST)
	public AjaxResult join(Join join) throws Exception {

		System.out.println("로그인 체크 컨트롤러 호출, 이메일값 : " + join.getEmail());

		// db에서 조회
		int checkedEmailCnt = joinDao.loginCheck(join);
		System.out.println(" 로긴체크하면서 다오에서 넘어온 값 : " + checkedEmailCnt);

		if (checkedEmailCnt > 0) {
			// 이메일 중복될 때
			return new AjaxResult("중복이메일있음", checkedEmailCnt);
		} else {
			// 이메일 중복 없을 때
			return new AjaxResult("ok", checkedEmailCnt);
		}

	}
	
	// 로그인
	@RequestMapping(value = "login", method = RequestMethod.POST)
	public AjaxResult login(Join join, HttpSession session) throws Exception {

		System.out.println("로그인 컨트롤러 호출 : " + join.getEmail());

		// 이메일을 먼저 체크
		int checkedEmailCnt = joinDao.loginCheck(join);
		System.out.println(" 이메일체크하면서 다오에서 넘어온 값 : " + checkedEmailCnt);

		if (checkedEmailCnt > 0) {
			// 체크된 이메일이 있을 때 이메일을 가지고 패스워드와 동시에 일치하는지
			int checkedLoginCnt = joinDao.EmailPwCheck(join);
			System.out.println("이메일과 패스워드가 일치하는 값 : " + checkedLoginCnt);

			if (checkedLoginCnt > 0) {
				// 이메일과 패스워드가 둘다 일치한다.

				// 등록된 유저 세션에 등록
				join = joinDao.selectUser(join);
				session.setAttribute("loginUser", join);

				return new AjaxResult("로그인되면 된다.", checkedLoginCnt);

			} else {
				// 패스워드가 일치하지 않는다.
				return new AjaxResult("패스워드가 일치하지 않습니다.", checkedLoginCnt);
			}

		} else {
			// 체크된 이메일이 없을때
			return new AjaxResult("가입된 이메일이 아닙니다.", checkedEmailCnt);
		}

	}

	@RequestMapping("userInfo")
	public AjaxResult userInfo(HttpSession session) throws Exception {

		System.out.println("userInfo 콘트롤러 호출");

		Join join = (Join) session.getAttribute("loginUser");

		System.out.println("세션 이메일 확인 : " + join.getEmail());
		System.out.println("세션 이름 확인 : " + join.getName());
		System.out.println("세션 파일 확인 : " + join.getPho());
		return new AjaxResult("success", join);
	}

	@RequestMapping(value = "userInfoUpdate", method = RequestMethod.POST)
	@ResponseBody
	public AjaxResult userInfoUpdate(Join join, @RequestParam(value = "file", required = false) MultipartFile mFile,HttpSession session)
			throws Exception {

		System.out.println("userInfoUpdate 콘트롤러 호출");
		System.out.println("넘어온유저넘버 : " + join.getuNo());
		System.out.println("넘어온이름 : " + join.getName());
		System.out.println("넘어온이메일 : " + join.getEmail());
		System.out.println("넘어온 현재 패스워드 : " + join.getPwd());
		System.out.println("넘어온 새 패스워드 : " + join.getNewPwd());

		// 유저사진
		String oriFileName = mFile.getOriginalFilename();

		System.out.println(oriFileName);

		if (oriFileName != null && !oriFileName.equals("")) {

			String realPath = servletContext.getRealPath("/attachfile/");
			String sdfPath = new SimpleDateFormat("yyyy/MM/dd/").format(new Date());
			String filePath = realPath + sdfPath;
			File file = new File(filePath);
			file.mkdirs();
				
			String ext = oriFileName.substring(oriFileName.lastIndexOf("."));
			String realFileName = UUID.randomUUID().toString() + ext;
			String saveFullFileName = filePath + "/" + realFileName;
			mFile.transferTo(new File(saveFullFileName));

			// 파일 경로
			join.setPho(sdfPath + realFileName);
		}

		// dao로 업데이트 전송 (사진 이름 이메일)
		joinDao.updateUserInfo(join);
		
		//전송 후 세션을 재 설정 
		session.setAttribute("loginUser", joinDao.selectUser(join));
		
		
		// 패스워드 변경시 - 유저번호와 현재 패스워드를 확인해서 새로운 패스워드등록

		return new AjaxResult("success", join);
	}

	// 로그아웃
	@RequestMapping("logout")
	public AjaxResult logout(HttpSession session) throws Exception {

		System.out.println("로그아웃 컨트롤러 호출");

		session.removeAttribute("loginUser");
		//session.invalidate();
		return new AjaxResult("success", null);
	}

}
