#pragma once
#include <memory>
#include <Combiner.h>
#include <Graphics/OpenGLContext/opengl_GLInfo.h>

namespace graphics {
	class CombinerProgram;
}

namespace glsl {
	class ShaderPart;
}

namespace glsl {

	class CombinerProgramBuilder
	{
	public:

		virtual graphics::CombinerProgram * buildCombinerProgram(Combiner & _color, Combiner & _alpha, const CombinerKey & _key) = 0;

		virtual const ShaderPart * getVertexShaderHeader() const = 0;

		virtual const ShaderPart * getFragmentShaderHeader() const = 0;

		virtual const ShaderPart * getFragmentShaderEnd() const = 0;

		virtual bool isObsolete() const = 0;
	};

}
