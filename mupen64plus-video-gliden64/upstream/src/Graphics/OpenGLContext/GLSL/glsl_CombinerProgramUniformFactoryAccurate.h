#pragma once
#include <Graphics/OpenGLContext/opengl_GLInfo.h>
#include "glsl_CombinerProgramImpl.h"
#include "glsl_CombinerProgramUniformFactory.h"

namespace glsl::Accurate {

	class CombinerProgramUniformFactory : public glsl::CombinerProgramUniformFactory
	{
	public:
		CombinerProgramUniformFactory(const opengl::GLInfo & _glInfo);

		void buildUniforms(GLuint _program,
							const CombinerInputs & _inputs,
							const CombinerKey & _key,
							UniformGroups & _uniforms) override;

	private:
		const opengl::GLInfo & m_glInfo;
	};

}
