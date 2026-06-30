# CLAUDE.md

이 문서는 `skin_recipe` 프로젝트에서 Claude Code가 작업할 때 따를 규칙이다.

---

## Project Context

이 프로젝트의 목적, 기술 스택, 아키텍처, 현재 구현 상태를 먼저 파악한다.

작업을 시작하기 전에 다음을 확인한다.

- README
- build 설정
- package 구조
- 주요 source directory
- test directory
- docker / infra / config 관련 파일

---

## Wiki First Rule

설계, 구현, 리팩토링, 장애 분석을 시작하기 전에 연결된 Obsidian LLM Wiki를 먼저 검색한다.

특히 다음 주제는 반드시 Wiki를 확인한다.

- Backend architecture
- Spring
- DDD
- Repository
- QueryDSL
- Authentication
- API design
- Testing
- Docker
- Deployment

관련 Wiki 문서가 있으면 그것을 기술적 맥락으로 사용한다.

관련 Wiki 문서가 없으면 일반 지식으로 진행하되, 작업 후 Wiki에 남길 가치가 있는지 판단한다.

---

## Priority

정보 우선순위는 다음과 같다.

1. 이 프로젝트의 명시적 요구사항
2. 이 프로젝트의 코드와 테스트
3. 연결된 LLM Wiki
4. 공식 문서
5. 일반적인 엔지니어링 지식
6. Claude의 사전 지식

프로젝트 코드와 Wiki가 충돌하면 프로젝트 코드를 우선한다.

---

## Working Style

작업은 다음 흐름을 따른다.

1. 관련 파일을 먼저 읽는다.
2. 필요한 경우 LLM Wiki를 검색한다.
3. 변경 계획을 짧게 제시한다.
4. 사용자의 승인을 받은 뒤 수정한다.
5. 변경 후 테스트 또는 검증 방법을 제안한다.
6. 재사용 가능한 지식이 생기면 Wiki 업데이트를 제안한다.

---

## Knowledge Update Rule

작업 중 다음에 해당하는 내용이 생기면 LLM Wiki 업데이트를 제안한다.

- 반복 가능한 구현 패턴
- 중요한 설계 결정
- 프로젝트 간 재사용 가능한 지식
- 장애 해결 경험
- 기존 Wiki와 충돌하는 새로운 판단
- Cookbook으로 남길 만한 절차
- ADR로 남길 만한 기술 선택

사용자 승인 없이 LLM Wiki를 수정하지 않는다.

---

## Implementation Rule

대규모 변경은 바로 수행하지 않는다.

먼저 다음을 제시한다.

- 현재 구조 분석
- 변경 대상 파일
- 변경 이유
- 예상 영향
- 검증 방법

작은 수정은 바로 수행할 수 있지만, 구조 변경이나 아키텍처 변경은 반드시 계획을 먼저 제시한다.