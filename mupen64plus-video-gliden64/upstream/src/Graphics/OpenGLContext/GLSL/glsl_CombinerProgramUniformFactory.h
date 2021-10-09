#pragma once
#include <Graphics/OpenGLContext/opengl_GLInfo.h>
#include "glsl_CombinerProgramImpl.h"

namespace glsl {

	class CombinerProgramUniformFactory
	{
	public:

		virtual void buildUniforms(GLuint _program,
							const CombinerInputs & _inputs,
							const CombinerKey & _key,
							UniformGroups & _uniforms) = 0;
	};

}
